import * as cdk from "aws-cdk-lib";
import * as iam from "aws-cdk-lib/aws-iam";
import { Construct } from "constructs";

export class EcsTaskExecutionRoleStack extends cdk.NestedStack {
  readonly role: iam.Role;

  constructor(
    scope: Construct,
    id: string,
    repositories: string[],
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const ecsTaskExecutionRole = new iam.Role(
      this,
      "ECSTaskExecutionRoleForInflux",
      {
        assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
        roleName: "ECSTaskExecutionRole",
        path: "/",
      }
    );

    ecsTaskExecutionRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
        ],
        resources: repositories.map((repository) => {
          return `arn:aws:ecr:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:repository/${repository}`;
        }),
      })
    );

    ecsTaskExecutionRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ],
        resources: ["arn:aws:logs:*:*:*"],
      })
    );
  }
}
