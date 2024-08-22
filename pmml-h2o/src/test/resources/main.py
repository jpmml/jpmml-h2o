from h2o import H2OFrame
from h2o.automl import H2OAutoML
from h2o.estimators.extended_isolation_forest import H2OExtendedIsolationForestEstimator
from h2o.estimators.gbm import H2OGradientBoostingEstimator
from h2o.estimators.glm import H2OGeneralizedLinearEstimator
from h2o.estimators.isolation_forest import H2OIsolationForestEstimator
from h2o.estimators.random_forest import H2ORandomForestEstimator
from h2o.estimators.stackedensemble import H2OStackedEnsembleEstimator
from h2o.estimators.xgboost import H2OXGBoostEstimator
from sklearn.preprocessing import KBinsDiscretizer

import h2o
import numpy
import sys

def load_csv(name, factors = []):
	df = h2o.import_file(path = "csv/" + name, header = 1, sep = ",", na_strings = ["N/A"])
	for factor in factors:
		df[factor] = df[factor].asfactor()
	return df

def store_csv(df, name):
	df.as_data_frame(use_pandas = True).to_csv("csv/" + name, index = False)

def split_columns(df):
	return (df.names[:-1], df.names[-1])

def store_mojo(model, name):
	model.download_mojo(path = "mojo/" + name)

h2o.init(nthreads = 1)

def make_automl():
	return H2OAutoML(exclude_algos = ["DeepLearning", "StackedEnsemble", "XGBoost"], max_runtime_secs = 10, seed = 42)

def make_stacked_ensemble(df, ntrees):
	gbm = H2OGradientBoostingEstimator(ntrees = ntrees, nfolds = 3, fold_assignment = "Modulo", keep_cross_validation_predictions = True, seed = 42)
	gbm.train(*split_columns(df), training_frame = df)
	drf = H2ORandomForestEstimator(ntrees = ntrees, nfolds = 3, fold_assignment = "Modulo", keep_cross_validation_predictions = True, seed = 42)
	drf.train(*split_columns(df), training_frame = df)
	return H2OStackedEnsembleEstimator(base_models = [gbm, drf], seed = 42)

datasets = []

if __name__ == "__main__":
	if len(sys.argv) > 1:
		datasets = (sys.argv[1]).split(",")
	else:
		datasets = ["Audit", "Auto", "Housing", "Iris", "Visit"]

#
# Binary classification
#

def load_audit(name):
	return load_csv(name + ".csv", ["Adjusted"])

def build_audit(df, classifier, name):
	classifier.train(*split_columns(df), training_frame = df)
	store_mojo(classifier, name + ".zip")
	adjusted = classifier.predict(df)
	adjusted.set_names(["defaultCalibration(Adjusted)", "probability(0)", "probability(1)"])

	adjusted_pmml = numpy.argmax(adjusted[["probability(0)", "probability(1)"]].as_data_frame(use_pandas = True).values, axis = 1)
	adjusted["Adjusted"] = H2OFrame(adjusted_pmml, column_types = ["categorical"])

	adjusted = adjusted[["Adjusted", "defaultCalibration(Adjusted)", "probability(0)", "probability(1)"]]
	store_csv(adjusted, name + ".csv")

if "Audit" in datasets:
	audit_df = load_audit("Audit")

	build_audit(audit_df, make_automl(), "AutoMLAudit")
	build_audit(audit_df, H2OGradientBoostingEstimator(ntrees = 31, seed = 42), "GBMAudit")
	build_audit(audit_df, H2OGeneralizedLinearEstimator(family = "binomial", lambda_ = 0, seed = 42), "GLMAudit")
	build_audit(audit_df, H2ORandomForestEstimator(ntrees = 1, sample_rate = 1, mtries = -2, max_depth = 6, seed = 42), "DecisionTreeAudit")
	build_audit(audit_df, H2ORandomForestEstimator(ntrees = 31, binomial_double_trees = True, seed = 42), "RandomForestAudit")
	build_audit(audit_df, H2OXGBoostEstimator(ntrees = 31, seed = 42), "XGBoostAudit")

	audit_df = load_audit("AuditNA")

	build_audit(audit_df, H2OGradientBoostingEstimator(ntrees = 1, sample_rate = 1, col_sample_rate = 1, max_depth = 6, seed = 42), "DecisionTreeAuditNA")
	build_audit(audit_df, H2OGradientBoostingEstimator(ntrees = 31, seed = 42), "GBMAuditNA")
	build_audit(audit_df, H2OGeneralizedLinearEstimator(family = "binomial", seed = 42), "GLMAuditNA")
	build_audit(audit_df, H2ORandomForestEstimator(ntrees = 31, binomial_double_trees = False, seed = 42), "RandomForestAuditNA")
	build_audit(audit_df, make_stacked_ensemble(audit_df, 11), "StackedEnsembleAuditNA")
	build_audit(audit_df, H2OXGBoostEstimator(ntrees = 31, seed = 42), "XGBoostAuditNA")

#
# Multi-class classification
#

def load_iris(name):
	return load_csv(name + ".csv")

def build_iris(df, classifier, name):
	classifier.train(*split_columns(df), training_frame = df)
	store_mojo(classifier, name + ".zip")
	species = classifier.predict(df)
	species.set_names(["Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"])
	store_csv(species, name + ".csv")

if "Iris" in datasets:
	iris_df = load_iris("Iris")

	build_iris(iris_df, make_automl(), "AutoMLIris")
	build_iris(iris_df, H2OGradientBoostingEstimator(ntrees = 11, seed = 42), "GBMIris")
	build_iris(iris_df, H2OGeneralizedLinearEstimator(family = "multinomial", seed = 42), "GLMIris")
	build_iris(iris_df, H2ORandomForestEstimator(ntrees = 1, sample_rate = 1, mtries = -2, max_depth = 2, seed = 42), "DecisionTreeIris")
	build_iris(iris_df, H2ORandomForestEstimator(ntrees = 11, seed = 42), "RandomForestIris")
	build_iris(iris_df, make_stacked_ensemble(iris_df, 5), "StackedEnsembleIris")
	build_iris(iris_df, H2OXGBoostEstimator(ntrees = 11, seed = 42), "XGBoostIris")

#
# Regression
#

def load_auto(name):
	return load_csv(name + ".csv", ["cylinders", "model_year", "origin"])

def build_auto(df, regressor, name):
	regressor.train(*split_columns(df), training_frame = df)
	store_mojo(regressor, name + ".zip")
	mpg = regressor.predict(df)
	mpg.set_names(["mpg"])
	store_csv(mpg, name + ".csv")

def build_ordinal_auto(df, classifier, name):
	classifier.train(*split_columns(df), training_frame = df)
	store_mojo(classifier, name + ".zip")
	mpg = classifier.predict(df)
	mpg.set_names(["mpg", "probability(0)", "probability(1)", "probability(2)"])
	store_csv(mpg, name + ".csv")

def _transform_mpg(auto_df):
	auto_y = auto_df["mpg"].as_data_frame()

	discretizer = KBinsDiscretizer(n_bins = 3, strategy = "quantile", encode = "ordinal")

	auto_y = discretizer.fit_transform(auto_y.values.reshape(-1, 1)).astype(int)

	auto_df["mpg"] = H2OFrame(auto_y, column_types = ["categorical"])

	return auto_df

if "Auto" in datasets:
	auto_df = load_auto("Auto")

	build_auto(auto_df, make_automl(), "AutoMLAuto")
	build_auto(auto_df, H2OGradientBoostingEstimator(ntrees = 17, nbins_cats = 5, seed = 42), "GBMAuto")
	build_auto(auto_df, H2OGeneralizedLinearEstimator(family = "gaussian", lambda_ = 0, seed = 42), "GLMAuto")
	build_auto(auto_df, H2ORandomForestEstimator(ntrees = 1, sample_rate = 1, mtries = -2, max_depth = 4, seed = 42), "DecisionTreeAuto")
	build_auto(auto_df, H2ORandomForestEstimator(ntrees = 17, nbins_cats = 3, seed = 42), "RandomForestAuto")
	build_auto(auto_df, H2OXGBoostEstimator(ntrees = 17, seed = 42), "XGBoostAuto")

	auto_df = _transform_mpg(auto_df)

	build_ordinal_auto(auto_df, H2OGeneralizedLinearEstimator(family = "ordinal", seed = 42), "GLMOrdinalAuto")

	auto_df = load_auto("AutoNA")

	build_auto(auto_df, H2OGradientBoostingEstimator(ntrees = 1, sample_rate = 1, col_sample_rate = 1, max_depth = 4, seed = 42), "DecisionTreeAutoNA")
	build_auto(auto_df, H2OGradientBoostingEstimator(ntrees = 17, seed = 42), "GBMAutoNA")
	build_auto(auto_df, H2OGeneralizedLinearEstimator(family = "gaussian", seed = 42), "GLMAutoNA")
	build_auto(auto_df, H2ORandomForestEstimator(ntrees = 17, nbins_cats = 3, seed = 42), "RandomForestAutoNA")
	build_auto(auto_df, make_stacked_ensemble(auto_df, 7), "StackedEnsembleAutoNA")
	build_auto(auto_df, H2OXGBoostEstimator(ntrees = 17, seed = 42), "XGBoostAutoNA")

def load_visit(name):
	return load_csv(name + ".csv", ["edlevel", "outwork", "female", "married", "kids"])

def build_visit(df, regressor, name):
	regressor.train(*split_columns(df), training_frame = df)
	store_mojo(regressor, name + ".zip")
	docvis = regressor.predict(df)
	docvis.set_names(["docvis"])
	store_csv(docvis, name + ".csv")

if "Visit" in datasets:
	visit_df = load_visit("Visit")

	build_visit(visit_df, H2OGradientBoostingEstimator(ntrees = 31, distribution = "poisson", seed = 42), "GBMPoissonVisit")
	build_visit(visit_df, H2OGradientBoostingEstimator(ntrees = 31, distribution = "tweedie", seed = 42), "GBMTweedieVisit")

	visit_df = load_visit("VisitNA")

	build_visit(visit_df, H2OGradientBoostingEstimator(ntrees = 31, distribution = "poisson", seed = 42), "GBMPoissonVisitNA")
	build_visit(visit_df, H2OGradientBoostingEstimator(ntrees = 31, distribution = "gamma", seed = 42), "GBMGammaVisitNA")

#
# Anomaly detection
#

def load_housing(name):
	return load_csv(name + ".csv")

def build_housing(df, estimator, name):
	estimator.train(*split_columns(df), training_frame = df)
	store_mojo(estimator, name + ".zip")
	score = estimator.predict(df)
	score.set_names(["anomalyScore", "meanPathLength"])
	store_csv(score, name + ".csv")

if "Housing" in datasets:
	housing_df = load_housing("Housing")

	build_housing(housing_df, H2OExtendedIsolationForestEstimator(ntrees = 17, extension_level = 4, seed = 42), "ExtendedIsolationForestHousing")
	build_housing(housing_df, H2OIsolationForestEstimator(ntrees = 17, max_depth = 11, seed = 42), "IsolationForestHousing")
