import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export type DashboardStackProps = cdk.NestedStackProps & {
  dashboardAlbListener: elbv2.ApplicationListener;
  ecsCluster: ecs.Cluster;
  ecsTaskExecRole: iam.Role;
};

export class DashboardStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: DashboardStackProps) {
    super(scope, id, props);

    const { dashboardAlbListener, ecsCluster, ecsTaskExecRole } = props!;

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'DashboardTaskDefinition', {
      cpu: 256,
      executionRole: ecsTaskExecRole,
      family: 'DashboardTaskDefinition',
      memoryLimitMiB: 512,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.ARM64
      }
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-dashboard');

    const containerPort = parseInt(process.env.DASHBOARD_SERVER_PORT!);

    taskDefinition.addContainer('DashboardContainer', {
      cpu: 256,
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'dashboard' }),
      memoryLimitMiB: 512,
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort }]
    });

    const dashboardService = new ecs.FargateService(this, 'DashboardEcsService', {
      assignPublicIp: true,
      cluster: ecsCluster,
      desiredCount: 1,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC }
    });

    dashboardService.registerLoadBalancerTargets({
      containerName: 'DashboardContainer',
      containerPort,
      newTargetGroupId: 'DashboardTargetGroup',
      listener: ecs.ListenerConfig.applicationListener(dashboardAlbListener, {
        protocol: elbv2.ApplicationProtocol.HTTP
      })
    });
  }
}
