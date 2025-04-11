JPMML-H2O [![Build Status](https://github.com/jpmml/jpmml-h2o/workflows/maven/badge.svg)](https://github.com/jpmml/jpmml-h2o/actions?query=workflow%3A%22maven%22)
=========

Java library and command-line application for converting [H2O.ai](https://www.h2o.ai/) models to PMML.

# Features #

### Supported MOJO types

* Supervised algorithms:
  * Automated Machine Learning (AutoML)
  * Distributed Random Forest (DRF):
    * [`DrfMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/drf/DrfMojoModel.html)
  * Gradient Boosting Machine (GBM):
    * [`GbmMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/gbm/GbmMojoModel.html)
  * Generalized Linear Model (GLM):
    * [`GlmMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/glm/GlmMojoModel.html)
    * [`GlmMultinomialMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/glm/GlmMultinomialMojoModel.html)
    * [`GlmOrdinalMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/glm/GlmOrdinalMojoModel.html)
  * Stacked Ensembles:
    * [`StackedEnsembleMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/ensemble/StackedEnsembleMojoModel.html)
  * XGBoost:
    * `XGBoostJavaMojoModel`
    * `XGBoostNativeMojoModel`
* Unsupervised algorithms:
  * Isolation Forest:
    * [`IsolationForestMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/isofor/IsolationForestMojoModel.html)
    * [`ExtendedIsolationForestMojoModel`](https://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/isoforextended/ExtendedIsolationForestMojoModel.html)

# Prerequisites #

* H2O.ai 3.34(.0.1) or newer
* Java 11 or newer

# Installation #

Enter the project root directory and build using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The build produces a library JAR file `pmml-h2o/target/pmml-h2o-1.3-SNAPSHOT.jar`, and an executable uber-JAR file `pmml-h2o-example/target/pmml-h2o-example-executable-1.3-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use H2O.ai to train a model.
2. Download the model in Model ObJect, Optimized (MOJO) data format to a file in a local filesystem.
3. Use the JPMML-H2O command-line converter application to turn the MOJO file to a PMML file.

### The H2O.ai side of operations

Using the [`h2o`](https://github.com/h2oai/h2o-3/tree/master/h2o-py) package to train a regression model for the example Boston housing dataset:

```python
from h2o import H2OFrame
from h2o.estimators.glm import H2OGeneralizedLinearEstimator
from pandas import DataFrame, Series
from sklearn.datasets import load_boston

import h2o
import pandas

boston = load_boston()

df = pandas.concat((DataFrame(data = boston.data, columns = boston.feature_names), Series(boston.target, name = "MEDV")), axis = 1)

h2o.connect()

df = H2OFrame(df)

glm = H2OGeneralizedLinearEstimator(family = "gaussian")
glm.train(boston.feature_names.tolist(), "MEDV", df)

glm.download_mojo(path = "mojo.zip")
```

### The Java side of operations

Converting the MOJO file `mojo.zip` to a PMML file `mojo.pmml`:
```
java -jar pmml-h2o-example/target/pmml-h2o-example-executable-1.3-SNAPSHOT.jar --mojo-input mojo.zip --pmml-output mojo.pmml
```

Getting help:
```
java -jar pmml-h2o-example/target/pmml-h2o-example-executable-1.3-SNAPSHOT.jar --help
```

# Documentation #

* [Converting Scikit-Learn H2O.ai pipelines to PMML](https://openscoring.io/blog/2023/07/17/converting_sklearn_h2o_pipeline_pmml/)
* [Training Scikit-Learn H2O.ai pipelines](https://openscoring.io/blog/2022/11/11/sklearn_h2o_pipeline/)

# License #

JPMML-H2O is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).

If you would like to use JPMML-H2O in a proprietary software project, then it is possible to enter into a licensing agreement which makes JPMML-H2O available under the terms and conditions of the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause) instead.

# Additional information #

JPMML-H2O is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)
