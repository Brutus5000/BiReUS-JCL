# BiReUS - Java Client Library

**Important note:** BiReUS is not stable yet. File format compatibility may break during development.

BiReUS-jcl is a java client library for [BiReUS](https://github.com/Brutus5000/BiReUS) with the same feature set as the python client.
For more details about BiReUS see the related [wiki](https://github.com/Brutus5000/BiReUS/wiki).


## Dependencies
* **Lombok** for less code overhead
* **Apache Commons IO and Compress** for handling (compressed) files
* **Jackson** for JSON (de-)serialization
* **jbsdiff** for patching file according to bsdiff4 (embedded since there is no working release available)
* **JGraphT** for resolving patch paths