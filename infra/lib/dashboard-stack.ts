import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class DashboardStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    dashboardAlbListener: elbv2.ApplicationListener,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'DashboardTaskDefinition',
      {
        family: 'DashboardTaskDefinition',
        executionRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64
        },
        memoryLimitMiB: 512,
        cpu: 256
      }
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      'ECRRepository',
      'stream-lines-dashboard'
    );

    taskDefinition.addContainer('DashboardContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort: 3000 }],
      memoryLimitMiB: 512,
      cpu: 256,
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'dashboard' })
    });

    const dashboardService = new ecs.FargateService(
      this,
      'DashboardEcsService',
      {
        cluster: ecsCluster,
        taskDefinition,
        vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
        desiredCount: 1,
        assignPublicIp: true
      }
    );

    dashboardService.registerLoadBalancerTargets({
      containerName: 'DashboardContainer',
      containerPort: 3000,
      newTargetGroupId: 'DashboardTargetGroup',
      listener: ecs.ListenerConfig.applicationListener(dashboardAlbListener, {
        protocol: elbv2.ApplicationProtocol.HTTP
      })
    });
  }
}
