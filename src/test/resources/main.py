from h2o.estimators.glm import H2OGeneralizedLinearEstimator

import h2o

def load_csv(name):
	return h2o.import_file(path = "csv/" + name, header = 1, sep = ",")

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

audit_df = load_csv("Audit.csv")
audit_df["Adjusted"] = audit_df["Adjusted"].asfactor()

audit_X, audit_y = split_columns(audit_df)

def build_audit(classifier, name):
	classifier.train(audit_X, audit_y, audit_df)
	store_mojo(classifier, name + ".zip")
	adjusted = classifier.predict(audit_df)
	adjusted.set_names(["Adjusted", "probability(0)", "probability(1)"])
	store_csv(adjusted, name + ".csv")

build_audit(H2OGeneralizedLinearEstimator(family = "binomial"), "GLMAudit")

#
# Multi-class classification
#

iris_df = load_csv("Iris.csv")

iris_X, iris_y = split_columns(iris_df)

def build_iris(classifier, name):
	classifier.train(iris_X, iris_y, iris_df)
	store_mojo(classifier, name + ".zip")
	species = classifier.predict(iris_df)
	species.set_names(["Species", "probability(setosa)", "probability(versicolor)", "probability(virginica)"])
	store_csv(species, name + ".csv")

build_iris(H2OGeneralizedLinearEstimator(family = "multinomial"), "GLMIris")

#
# Regression
#

auto_df = load_csv("Auto.csv")

auto_X, auto_y = split_columns(auto_df)

def build_auto(regressor, name):
	regressor.train(auto_X, auto_y, auto_df)
	store_mojo(regressor, name + ".zip")
	mpg = regressor.predict(auto_df)
	mpg.set_names(["mpg"])
	store_csv(mpg, name + ".csv")

build_auto(H2OGeneralizedLinearEstimator(family = "gaussian"), "GLMAuto")
