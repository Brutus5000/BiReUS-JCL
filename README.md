# BiReUS - Java Client Library

**Important note:** BiReUS is not stable yet. File format compatibility may break during development.

BiReUS-jcl is a java client library for [BiReUS](https://github.com/Brutus5000/BiReUS) with the same feature set as the python client.
For more details about BiReUS see the related [wiki](https://github.com/Brutus5000/BiReUS/wiki).

[![Build status](https://travis-ci.org/Brutus5000/BiReUS-JCL.svg?branch=master)](https://travis-ci.org/Brutus5000/BiReUS-jcl)

**Attention Non-Windows users:**
To run the unit tests without failing, all .txt files in src/test/resources need to have their original line endings (see .travis.yml for fix).

## Dependencies
* **Lombok** for less code overhead
* **Apache Commons IO and Compress** for handling (compressed) files
* **Jackson** for JSON (de-)serialization
* **jbsdiff** for patching file according to bsdiff4 (embedded since there is no working release available)
* **JGraphT** for resolving patch paths