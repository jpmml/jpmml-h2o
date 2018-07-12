from h2o.estimators.glm import H2OGeneralizedLinearEstimator
from h2o.estimators.xgboost import H2OXGBoostEstimator

import h2o

def load_csv(name):
	return h2o.import_file(path = "csv/" + name, header = 1, sep = ",", na_strings = ["N/A"])

def store_csv(df, name):
	df.as_data_frame(use_pandas = True).to_csv("csv/" + name, index = False)

def split_columns(df):
	return (df.names[:-1], df.names[-1])

def store_mojo(model, name):
	model.download_mojo(path = "mojo/" + name)

h2o.connect()

#
# Binary classification
#

def load_audit(name):
	df = load_csv(name)
	df["Adjusted"] = df["Adjusted"].asfactor()
	return df

def build_audit(df, classifier, name):
	classifier.train(*split_columns(df), training_frame = df)
	store_mojo(classifier, name + ".zip")
	adjusted = classifier.predict(df)
	adjusted.set_names(["Adjusted", "probability(0)", "probability(1)"])
	store_csv(adjusted, name + ".csv")

audit_df = load_audit("Audit.csv")

build_audit(audit_df, H2OGeneralizedLinearEstimator(family = "binomial", lambda_ = 0), "GLMAudit")
build_audit(audit_df, H2OXGBoostEstimator(ntrees = 31), "XGBoostAudit")

audit_df = load_audit("AuditNA.csv")

build_audit(audit_df, H2OGeneralizedLinearEstimator(family = "binomial"), "GLMAuditNA")

#
# Multi-class classification
#

def load_iris(name):
	return load_csv(name)

def build_iris(df, classifier, name):
	classifier.train(*split_columns(df), training_frame = df)
	store_mojo(classifier, name + ".zip")
	species = classifier.predict(df)
	species.set_names(["Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"])
	store_csv(species, name + ".csv")

iris_df = load_iris("Iris.csv")

build_iris(iris_df, H2OGeneralizedLinearEstimator(family = "multinomial"), "GLMIris")
build_iris(iris_df, H2OXGBoostEstimator(ntrees = 11), "XGBoostIris")

#
# Regression
#

def load_auto(name):
	df = load_csv(name)
	df["cylinders"] = df["cylinders"].asfactor()
	df["origin"] = df["origin"].asfactor()
	return df

def build_auto(df, regressor, name):
	regressor.train(*split_columns(df), training_frame = df)
	store_mojo(regressor, name + ".zip")
	mpg = regressor.predict(df)
	mpg.set_names(["mpg"])
	store_csv(mpg, name + ".csv")

auto_df = load_auto("Auto.csv")

build_auto(auto_df, H2OGeneralizedLinearEstimator(family = "gaussian", lambda_ = 0), "GLMAuto")
build_auto(auto_df, H2OXGBoostEstimator(ntrees = 17), "XGBoostAuto")

auto_df = load_auto("AutoNA.csv")

build_auto(auto_df, H2OGeneralizedLinearEstimator(family = "gaussian"), "GLMAutoNA")
