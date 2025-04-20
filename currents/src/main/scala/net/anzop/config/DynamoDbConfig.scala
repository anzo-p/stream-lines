package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

object DynamoDbConfig {

  val tableName: String = sys.env.getOrThrow("CURRENTS_DYNAMODB_TABLE_NAME", "CURRENTS_DYNAMODB_TABLE_NAME is not set")

  val client: DynamoDbClient = DynamoDbClient
    .builder()
    .region(Region.EU_WEST_1)
    .build()

  def close(): Unit = client.close()
}
