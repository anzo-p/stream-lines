import {
  DescribeServicesCommand,
  ECSClient,
  ListClustersCommand,
  ListServicesCommand,
  UpdateServiceCommand
} from '@aws-sdk/client-ecs';

const client = new ECSClient({});

export const handler = async (): Promise<void> => {
  try {
    const listClusters = await client.send(new ListClustersCommand({}));

    for (const clusterArn of listClusters.clusterArns || []) {
      const listServices = await client.send(new ListServicesCommand({ cluster: clusterArn }));
      const serviceArns = listServices.serviceArns || [];

      if (serviceArns.length === 0) continue;

      const describeDetails = await client.send(
        new DescribeServicesCommand({
          cluster: clusterArn,
          include: ['TAGS'],
          services: serviceArns
        })
      );

      for (const service of describeDetails.services || []) {
        const isAutoStop = service.tags?.some((t) => t.key === 'AutoStop' && t.value === 'true');

        if (isAutoStop && service.desiredCount !== 0) {
          console.log(`Scaling down ${service.serviceName}`);
          await client.send(
            new UpdateServiceCommand({
              cluster: clusterArn,
              desiredCount: 0,
              service: service.serviceArn
            })
          );
        }
      }
    }
  } catch (error) {
    console.error('Error scaling down ECS services:', error);
    throw error;
  }
};
