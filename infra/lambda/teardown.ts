import { CloudFormationClient, DeleteStackCommand, DescribeStacksCommand } from '@aws-sdk/client-cloudformation';

const cf = new CloudFormationClient({});

export const handler = async (event: { stackName?: string }) => {
  const stackName = event?.stackName;
  if (!stackName) {
    throw new Error('Missing event.stackName');
  }

  const desc = await cf.send(new DescribeStacksCommand({ StackName: stackName }));
  const tags = desc.Stacks?.[0]?.Tags ?? [];
  const isAutoTeardownDenied = tags.some((t) => t.Key === 'AutoTeardown' && t.Value === 'false');
  if (isAutoTeardownDenied) {
    console.log(`Will not tear down stack ${stackName} as AutoTeardown tag was set to false.`);
    return;
  }

  console.log('Deleting stack:', stackName);
  await cf.send(new DeleteStackCommand({ StackName: stackName }));
};
