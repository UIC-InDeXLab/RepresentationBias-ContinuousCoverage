# ContinuousCoverage

Coverage discovery for continuous data

# Publications to cite:
[1] Abolfazl Asudeh, Nima Shahbazi, Zhongjun Jin, H. V. Jagadish. **Identifying Insufficient Data Coverage for Ordinal Continuous-Valued Attributes**. SIGMOD, 2021, ACM.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

* [Maven 3.6](https://maven.apache.org/install.html)
* [Java 8 JDK](https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html)


### Installing (Console)

In console
```bash
mvn clean install
```

### Installing (Eclipse)

In Eclipse or other IDE, all packages should be automatically installed once [imported](https://vaadin.com/learn/tutorials/import-maven-project-eclipse). ished


## Running the tests 

Explain how to run the automated tests for this system

### Command line arguments

Command line arguments to use when running test scripts

| Option | Descriptions                    | Has arguments | Allow multiple values |
|--------|--------------------------------|---------------|-----------------------|
| -a     | selected attribute values      | Yes           | Yes                   |
| -e     | epsilon values                 | Yes           | Yes                   |
| -h     | show help                      | No            |                       |
| -i     | input dataset data file name   | Yes           | No                    |
| -k     | k values                       | Yes           | Yes                   |
| -n     | number of query points              | Yes           | Yes                   |
| -o     | if store test result in a file | No            |                       |
| -p     | number of repeats              | Yes           | No                    |
| -phi   | phi values                     | Yes           | Yes                   |
| -r     | rho values                     | Yes           | Yes                   |
| -s     | input dataset schema file name | Yes           | No                    |


### Run tests from console

#### Accuracy Test

Format

```bash
mvn -e exec:java@accuracy -Dexec.args="{command-line-arguments}"
```

Example

```bash
mvn -e exec:java@accuracy -Dexec.args="-i data/iris.data -s data/iris.schema -a sepalLength sepalWidth petalLength -k 3 -r 0.05 0.1 0.15 -n 2000 -p 100 -e 0.1 0.2 -phi 0.1 0.2"
```

#### Efficiency Test

Format

```bash
mvn -e exec:java@accuracy -Dexec.args="{command-line-arguments}"
```

Example

```bash
mvn -e exec:java@efficiency -Dexec.args="-i data/iris.data -s data/iris.schema -a sepalLength sepalWidth -k 2 -r 0.05 0.1 0.15 -n 1000 2000 -p 100"
```

### From Eclipse

In Eclipse or other IDE, [run](https://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-launching_java_program.htm) `src/test/java/umichdb/coverage2/TestCoverageChecker.java`



## Built With

* [Smile](https://haifengl.github.io/) - Java-based Machine Learning Pacakge
* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **[Zhongjun Jin](https://github.com/markjin1990)**
* **[Nima Shahbazi](https://www.linkedin.com/in/neemashahbazi/)**
* **[Abolfazl Asudeh](https://github.com/asudeh)**

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
