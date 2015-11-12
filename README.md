


h3. yfiles

Internally, the debug L2 diagram tool uses the third party library yFiles to 
generate a png file. 
Unfortunately, this library is only offered under commercial
license. At the moment, only the evaluation copy of this library is used to generate a graph.
If the graph file was not written to the specified path and the message
"The evaluation time of yFiles for Java has expired" is returned, the 
artifact com.yworks:yfiles:2.12 must be replaced by a new
evaluation copy in order to be able to generate a graph file.

A new evaluation copy can be requested at http://www.yworks.com/en/products_yfiles_eval.php

```
  mvn install:install-file                   \
    -Dfile=y.jar                             \
    -DgroupId=com.yworks                     \
    -DartifactId=yfiles                      \
    -Dversion=2.12                           \
    -Dpackaging=jar                          \
    -DgeneratePom=true
```

