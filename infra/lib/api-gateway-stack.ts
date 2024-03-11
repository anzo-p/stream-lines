import * as cdk from 'aws-cdk-lib';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as apigw2 from 'aws-cdk-lib/aws-apigatewayv2';
import * as apigw2_integr from 'aws-cdk-lib/aws-apigatewayv2-integrations';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export class WebSocketApiGatewayStack extends cdk.NestedStack {
  readonly webSocketApiGatewayStageProdArn: string;
  readonly webSocketApiGatewayConnectionsUrl: string;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const roleWebSocketHandlerLambda = new iam.Role(
      this,
      'LambdaRoleWebSocketHandler',
      {
        assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName(
            'service-role/AWSLambdaBasicExecutionRole'
          )
        ]
      }
    );

    roleWebSocketHandlerLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: [
          'logs:CreateLogGroup',
          'logs:CreateLogStream',
          'logs:PutLogEvents'
        ],
        effect: iam.Effect.ALLOW,
        resources: ['arn:aws:logs:*:*:*']
      })
    );

    roleWebSocketHandlerLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: ['dynamodb:PutItem', 'dynamodb:DeleteItem'],
        effect: iam.Effect.ALLOW,
        resources: [
          `arn:aws:dynamodb:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:table/${process.env.WS_CONNS_TABLE_NAME}`,
          `arn:aws:dynamodb:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:table/${process.env.WS_CONNS_BY_SYMBOL_INDEX}/index`
        ]
      })
    );

    const bucketWebSocketHandlerLambda = s3.Bucket.fromBucketName(
      this,
      'LambdaCodeBucket',
      `${process.env.S3_BUCKET_LAMBDAS}`
    );

    const webSocketHandlerLambda = new lambda.Function(
      this,
      'WebSocketHandler',
      {
        functionName: 'ApiGatewayWebSocketHandler',
        runtime: lambda.Runtime.NODEJS_20_X,
        handler: 'index.handler',
        code: lambda.Code.fromBucket(
          bucketWebSocketHandlerLambda,
          `${process.env.S3_KEY_WS_CONN_HANDLER}`
        ),
        role: roleWebSocketHandlerLambda,
        environment: {
          WS_CONNS_TABLE_NAME: `${process.env.WS_CONNS_TABLE_NAME}`
        }
      }
    );

    const webSocketApiGateway = new apigw2.WebSocketApi(
      this,
      'WebSocketApiGateway',
      {
        connectRouteOptions: {
          integration: new apigw2_integr.WebSocketLambdaIntegration(
            'WebSocketConnectionRoute',
            webSocketHandlerLambda
          )
        },
        disconnectRouteOptions: {
          integration: new apigw2_integr.WebSocketLambdaIntegration(
            'WebSocketDisconnectionRoute',
            webSocketHandlerLambda
          )
        },
        defaultRouteOptions: {
          integration: new apigw2_integr.WebSocketLambdaIntegration(
            'WebSocketDefaultRoute',
            webSocketHandlerLambda
          )
        }
      }
    );

    const webSocketApiGatewayStageProd = new apigw2.WebSocketStage(
      this,
      'WebSocketStageProd',
      {
        webSocketApi: webSocketApiGateway,
        stageName: 'prod',
        autoDeploy: true
      }
    );

    this.webSocketApiGatewayStageProdArn = this.formatArn({
      service: 'execute-api',
      resourceName: `${webSocketApiGatewayStageProd.stageName}/POST/@connections/*`,
      resource: webSocketApiGateway.apiId
    });

    webSocketHandlerLambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['execute-api:ManageConnections'],
        resources: [this.webSocketApiGatewayStageProdArn]
      })
    );

    this.webSocketApiGatewayConnectionsUrl =
      `https://${webSocketApiGateway.apiId}.execute-api.` +
      `${process.env.AWS_REGION}.amazonaws.com/` +
      `${webSocketApiGatewayStageProd.stageName}`;

    const apigwCustomDomain = new apigw2.DomainName(
      this,
      'WebSocketApiGatewayDomainName',
      {
        domainName: `${process.env.WS_API_DOMAIN_NAME}`,
        certificate: acm.Certificate.fromCertificateArn(
          this,
          'Certificate',
          `arn:aws:acm:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:certificate/${process.env.ACM_BACKEND_CERT}`
        )
      }
    );

    new apigw2.ApiMapping(this, 'WebSocketApiMapping', {
      api: webSocketApiGateway,
      domainName: apigwCustomDomain,
      stage: webSocketApiGatewayStageProd
    });

    new route53.ARecord(this, 'WebSocketApiGatewayAliasRecord', {
      zone: route53.HostedZone.fromLookup(this, 'HostedZone', {
        domainName: 'anzop.net'
      }),
      recordName: `${process.env.WS_API_SUBDOMAIN}`,
      target: route53.RecordTarget.fromAlias(
        new targets.ApiGatewayv2DomainProperties(
          apigwCustomDomain.regionalDomainName,
          apigwCustomDomain.regionalHostedZoneId
        )
      )
    });
  }
}
