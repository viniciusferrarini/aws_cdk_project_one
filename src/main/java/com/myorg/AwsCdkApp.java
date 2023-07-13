package com.myorg;

import software.amazon.awscdk.App;

public class AwsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        final VpcStack vpc = new VpcStack(app, "Vpc");

        final ClusterStack cluster = new ClusterStack(app, "Cluster", vpc.getVpc());
        cluster.addDependency(vpc); // Força ao CDk criar a VPC antes do cluster

        final RdsStack rds = new RdsStack(app, "Rds", vpc.getVpc());
        cluster.addDependency(vpc);

        final ServiceOneStack serviceOne = new ServiceOneStack(app, "ServiceOne", cluster.getCluster());
        serviceOne.addDependency(cluster);
        serviceOne.addDependency(rds);

        app.synth();
    }
}

