package com.anzop.appconfig

import com.anzop.helpers.Extensions.EnvOps
import org.apache.flink.kinesis.shaded.org.apache.flink.connector.aws.config.AWSConfigConstants

import java.util.Properties

object KinesisProps {

  def make(): Properties = {
    val props = new Properties()
    props.setProperty(AWSConfigConstants.AWS_REGION, sys.env.getOrThrow("AWS_REGION", "AWS_REGION is not set"))
    props.setProperty("streamName", sys.env.getOrThrow("KINESIS_DOWNSTREAM_NAME", "KINESIS_DOWNSTREAM_NAME is not set"))
    props
  }
}
