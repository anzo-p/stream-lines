export type ReceivedMessage = {
  subscribeTo: string[];
};

export function isReceivedMessage(obj: any): obj is ReceivedMessage {
  return obj && Array.isArray(obj.subscribeTo);
}
