JPMML-H2O
=========

Java library and command-line application for converting [H2O.ai](https://www.h2o.ai/) models to PMML.

# Features #

* Supported MOJO types:
  * [`GlmMojoModel`](http://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/glm/GlmMojoModel.html)
  * [`GlmMultinomialMojoModel`](http://docs.h2o.ai/h2o/latest-stable/h2o-genmodel/javadoc/hex/genmodel/algos/glm/GlmMultinomialMojoModel.html)
  * `XGBoostJavaMojoModel`
  * `XGBoostNativeMojoModel`

# Prerequisites #

* H2O 3.18.0 or newer
* Java 1.8 or newer

# Installation #

Enter the project root directory and build using [Apache Maven](http://maven.apache.org/):
```
mvn clean install
```

The build produces an executable uber-JAR file `target/jpmml-h2o-executable-1.0-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use H2O to train a model.
2. Download the model in Model ObJect, Optimized (MOJO) data format to a file in a local filesystem.
3. Use the JPMML-H2O command-line converter application to turn the MOJO file to a PMML file.

### The H2O side of operations

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
java -jar target/jpmml-h2o-executable-1.0-SNAPSHOT.jar --mojo-input mojo.zip --pmml-output mojo.pmml
```

Getting help:
```
java -jar target/jpmml-h2o-executable-1.0-SNAPSHOT.jar --help
```

# License #

JPMML-H2O is dual-licensed under the [GNU Affero General Public License (AGPL) version 3.0](http://www.gnu.org/licenses/agpl-3.0.html), and a commercial license.

# Additional information #

JPMML-H2O is developed and maintained by Openscoring Ltd, Estonia.

Interested in using JPMML software in your application? Please contact [info@openscoring.io](mailto:info@openscoring.io)
