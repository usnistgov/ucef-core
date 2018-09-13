# org.c2w-examples-structured

This subproject is the example of a "simple Helloworld" workflow.

To perform these steps, you need access to `cpswtng-meta` repo and the `HelloWorld.webgmex` seed.

## HelloWorld\_Java\_Tutorial\_1\_FederatesExporter

This folder is the result of the `Federates Exporter` with the following variables:

```
    version = 0.0.1
    groupId = org.c2wt.example
    org.cpswt version = 0.6.0-SNAPSHOT
    archiva repo base url = http://cpswtng_archiva:8080
```

Needed steps: maven clean -> compile -> deploy

## HelloWorld\_Java\_Tutorial\_2\_Implementation

This folder is the implementation of the interfaces generated above.

## HelloWorld\_Java\_Tutorial\_3\_DeploymentExporter

This folder is the result of the `Deployment Exporter` with the following variables:

```
    version = 0.0.1
    groupId = org.c2wt.example
    org.cpswt version = 0.6.0-SNAPSHOT
    archiva repo base url = http://cpswtng_archiva:8080
```