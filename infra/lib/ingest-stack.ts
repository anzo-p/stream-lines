import * as cdk from "aws-cdk-lib";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as iam from "aws-cdk-lib/aws-iam";
import { Construct } from "constructs";

export class IngestStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const ingestContainerRepository = "control-tower-ingest";

    const sg = new ec2.SecurityGroup(this, "IngestSecurityGroup", {
      vpc: ecsCluster.vpc,
      allowAllOutbound: true,
    });

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      "IngestTaskDefinition",
      {
        family: "IngestTaskDefinition",
        executionRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64,
        },
        memoryLimitMiB: 512,
        cpu: 256,
      }
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      "ECRRepository",
      ingestContainerRepository
    );

    taskDefinition.addContainer("IngestContainer", {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, "latest"),
      memoryLimitMiB: 512,
      cpu: 256,
      environment: {
        ALPACA_API_KEY: `${process.env.INGEST_ALPACA_API_KEY}`,
        ALPACA_API_SECRET: `${process.env.INGEST_ALPACA_API_SECRET}`,
        AWS_ACCESS_KEY_ID: `${process.env.AWS_ACCESS_KEY_ID}`,
        AWS_SECRET_ACCESS_KEY: `${process.env.AWS_SECRET_ACCESS_KEY}`,
        AWS_REGION: `${process.env.AWS_REGION}`,
        KINESIS_UPSTREAM_NAME: `${process.env.INGEST_KINESIS_UPSTREAM_NAME}`,
        MAX_WS_READS_PER_SEC: `${process.env.INGEST_MAX_WS_READS_PER_SEC}`,
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: "ingest" }),
    });

    new ecs.FargateService(this, "IngestEcsService", {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [sg],
      desiredCount: 1,
      assignPublicIp: true,
    });
  }
}
