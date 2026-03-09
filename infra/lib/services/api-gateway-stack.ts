import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as apigw2 from 'aws-cdk-lib/aws-apigatewayv2';
import * as apigw2_integr from 'aws-cdk-lib/aws-apigatewayv2-integrations';
import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import { Construct } from 'constructs';

export type WebSocketApiGatewayStackProps = cdk.NestedStackProps & {
  appBucket: s3.IBucket;
  acmApigwCertId: string;
  wsApiDomainName: string;
  wsApiSubdomain: string;
  wsConnsHandlerLambdaFullPath: string;
  wsConnsTableName: string;
};

export class WebSocketApiGatewayStack extends cdk.NestedStack {
  readonly wsApiGatewayStageProdArn: string;
  readonly wsApiGatewayConnectionsUrl: string;

  constructor(scope: Construct, id: string, props: WebSocketApiGatewayStackProps) {
    super(scope, id, props);

    const {
      appBucket,
      acmApigwCertId,
      wsApiDomainName,
      wsApiSubdomain,
      wsConnsHandlerLambdaFullPath,
      wsConnsTableName
    } = props;

    const accountId = cdk.Stack.of(this).account;
    const region = cdk.Stack.of(this).region;

    const roleWebSocketHandlerLambda = new iam.Role(this, 'WebSocketHandlerLambdaRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')]
    });

    const table = dynamodb.Table.fromTableName(this, 'WsConnectionsTable', wsConnsTableName);
    table.grantWriteData(roleWebSocketHandlerLambda);

    const logGroup = new logs.LogGroup(this, 'WebSocketHandlerLogGroup', {
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    const webSocketHandlerLambda = new lambda.Function(this, 'WebSocketHandlerLambda', {
      code: lambda.Code.fromBucket(appBucket, wsConnsHandlerLambdaFullPath),
      environment: {
        WS_CONNS_TABLE_NAME: wsConnsTableName
      },
      functionName: 'ApiGatewayWebSocketHandler',
      handler: 'index.handler',
      logGroup,
      role: roleWebSocketHandlerLambda,
      runtime: lambda.Runtime.NODEJS_20_X
    });

    const webSocketApiGateway = new apigw2.WebSocketApi(this, 'WebSocketApiGateway', {
      connectRouteOptions: {
        integration: new apigw2_integr.WebSocketLambdaIntegration('ConnectionRoute', webSocketHandlerLambda)
      },
      disconnectRouteOptions: {
        integration: new apigw2_integr.WebSocketLambdaIntegration('DisconnectionRoute', webSocketHandlerLambda)
      },
      defaultRouteOptions: {
        integration: new apigw2_integr.WebSocketLambdaIntegration('DefaultRoute', webSocketHandlerLambda)
      }
    });

    const stageProd = new apigw2.WebSocketStage(this, 'StageProd', {
      autoDeploy: true,
      stageName: 'prod',
      webSocketApi: webSocketApiGateway
    });

    this.wsApiGatewayStageProdArn = this.formatArn({
      resource: webSocketApiGateway.apiId,
      resourceName: `${stageProd.stageName}/POST/@connections/*`,
      service: 'execute-api'
    });

    webSocketHandlerLambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['execute-api:ManageConnections'],
        resources: [this.wsApiGatewayStageProdArn]
      })
    );

    this.wsApiGatewayConnectionsUrl = `https://${webSocketApiGateway.apiId}.execute-api.${region}.amazonaws.com/${stageProd.stageName}`;

    /*
      Handler lambda must exist before api gateway to handle its routes
      and only after api gatway exists and has a stage, may we have the @connections URL.
      The event.requestContext, that the lambda will receive, cannot be used
      because it is now overridden with a custom domain over secured wss protocol.
    */
    webSocketHandlerLambda.addEnvironment('API_GW_CONNECTIONS_URL', this.wsApiGatewayConnectionsUrl);

    const apigwCustomDomain = new apigw2.DomainName(this, 'CustomDomainName', {
      certificate: acm.Certificate.fromCertificateArn(
        this,
        'Certificate',
        `arn:aws:acm:${region}:${accountId}:certificate/${acmApigwCertId}`
      ),
      domainName: wsApiDomainName
    });

    new apigw2.ApiMapping(this, 'ApiMapping', {
      api: webSocketApiGateway,
      domainName: apigwCustomDomain,
      stage: stageProd
    });

    new route53.ARecord(this, 'WebSocketApiGatewayAliasRecord', {
      recordName: wsApiSubdomain,
      target: route53.RecordTarget.fromAlias(
        new targets.ApiGatewayv2DomainProperties(
          apigwCustomDomain.regionalDomainName,
          apigwCustomDomain.regionalHostedZoneId
        )
      ),
      zone: route53.HostedZone.fromLookup(this, 'HostedZone', {
        domainName: 'anzop.net'
      })
    });
  }
}
