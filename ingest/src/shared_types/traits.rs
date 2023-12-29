use prost::Message;

use crate::error_handling::ProcessError;
use crate::protobuf::{QuotationMessageProto, TradeMessageProto};
use crate::shared_types::protobuf_conversion::{quotation_message_to_protobuf, trade_message_to_protobuf};
use crate::shared_types::types::{QuotationMessage, TradeMessage};
use crate::shared_types::ReceivedMessage;

pub(crate) trait ToProtobuf {
    fn to_protobuf(&self) -> Result<Vec<u8>, ProcessError>;
}

trait ToProtobufMessage {
    type ProtobufType;
    fn to_protobuf_message(&self) -> Result<Self::ProtobufType, ProcessError>;
}

impl ToProtobuf for QuotationMessage {
    fn to_protobuf(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_message()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl ToProtobuf for TradeMessage {
    fn to_protobuf(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_message()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl ToProtobuf for ReceivedMessage {
    fn to_protobuf(&self) -> Result<Vec<u8>, ProcessError> {
        match self {
            ReceivedMessage::QuotationMessage(data) => data.to_protobuf(),
            ReceivedMessage::TradeMessage(data) => data.to_protobuf(),
        }
    }
}

impl ToProtobufMessage for QuotationMessage {
    type ProtobufType = QuotationMessageProto;

    fn to_protobuf_message(&self) -> Result<Self::ProtobufType, ProcessError> {
        quotation_message_to_protobuf(self)
    }
}

impl ToProtobufMessage for TradeMessage {
    type ProtobufType = TradeMessageProto;

    fn to_protobuf_message(&self) -> Result<Self::ProtobufType, ProcessError> {
        trade_message_to_protobuf(self)
    }
}
