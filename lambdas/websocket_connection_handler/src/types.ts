export type SubscriptionMessage = {
  subscribeTo: string[];
};

export function isSubscriptionMessage(obj: any): obj is SubscriptionMessage {
  return obj && Array.isArray(obj.subscribeTo);
}
