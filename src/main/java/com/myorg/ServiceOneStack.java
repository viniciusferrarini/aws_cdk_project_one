package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

public class ServiceOneStack extends Stack {

    public ServiceOneStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public ServiceOneStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        final ApplicationLoadBalancedFargateService serviceOne = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB_ONE")
                .serviceName("service-one")
                .cluster(cluster)
                .cpu(512)
                .desiredCount(2)
                .listenerPort(8080)
                .assignPublicIp(true) //utilizado devido a vpc ser criada sem natGateways
                .memoryLimitMiB(1024)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("aws_project_one")
                                .image(ContainerImage.fromRegistry("viniciusferrarini/aws_project_one:1.0.0"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                                .logGroup(LogGroup.Builder.create(this, "ServiceOneLogGroup")
                                                        .logGroupName("ServiceOne")
                                                        .removalPolicy(RemovalPolicy.DESTROY)
                                                        .build())
                                                .streamPrefix("ServiceOne")
                                        .build()))
                                .build())
                .publicLoadBalancer(true)
                .build();

        serviceOne.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                        .path("/actuator/health")
                        .port("8080")
                        .healthyHttpCodes("200")
                .build());

        final ScalableTaskCount scalableTaskCount = serviceOne.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("ServiceOneAutoScaling", CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(60)) // Tempo em segundos para criar novas instancias baseado no uso de CPU
                        .scaleOutCooldown(Duration.seconds(60)) // Tempo em segundos para remover instancias baseado no uso de CPU
                .build());
    }
}
