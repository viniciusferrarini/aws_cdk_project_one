package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;

public class RdsStack extends Stack {

    public RdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        CfnParameter dbPassword = CfnParameter.Builder.create(this, "dbPassword")
                .type("String")
                .description("The RDS instance password")
                .build();

        final ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        DatabaseInstance databaseInstance = DatabaseInstance.Builder
                .create(this, "RdsOne")
                .instanceIdentifier("aws-project-one-db")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_5_7)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin", CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.plainText(dbPassword.getValueAsString()))
                        .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        CfnOutput.Builder.create(this, "rds-endpoint")
                .exportName("rds-endpoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "rds-password")
                .exportName("rds-password")
                .value(dbPassword.getValueAsString())
                .build();

    }

}
