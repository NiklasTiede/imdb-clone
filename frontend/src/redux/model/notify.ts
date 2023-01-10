import { createModel } from "@rematch/core";
import { RootModel } from "../models";
import { VariantType } from "notistack";

export enum AlertSeverity {
  ERROR = "error",
  WARNING = "warning",
  INFO = "info",
  SUCCESS = "success",
}

export type Notification = {
  key: number;
  message: string;
  variant: VariantType;
};

export interface NotificationState {
  notifications: Notification[];
}

export const notify = createModel<RootModel>()({
  state: {
    notifications: [],
  } as NotificationState,
  reducers: {
    appendNotification: (state, payload: Notification) =>
      reduce(state, {
        notifications: [...state.notifications, payload],
      }),
    removeNotification: (state, payload: Notification) =>
      reduce(state, {
        notifications: state.notifications.filter((e) => e.key != payload.key),
      }),
  },
  effects: (dispatch) => ({
    addNotification: (payload) => {
      dispatch.notify.appendNotification({
        key: Math.floor(Math.random() * 10000),
        message: payload.message,
        variant: payload.variant,
      });
    },
    info: (message: string) => {
      dispatch.notify.addNotification({
        message: message,
        variant: AlertSeverity.INFO,
      });
    },
    warn: (message: string) => {
      dispatch.notify.addNotification({
        message: message,
        variant: AlertSeverity.WARNING,
      });
    },
    error: (message: string) => {
      dispatch.notify.addNotification({
        message: message,
        variant: AlertSeverity.ERROR,
      });
    },
    success: (message: string) => {
      dispatch.notify.addNotification({
        message: message,
        variant: AlertSeverity.SUCCESS,
      });
    },
    removeNotificationById: (notification: Notification) => {
      dispatch.notify.removeNotification(notification);
    },
  }),
});

export const reduce = <T>(s: T, v: any) => Object.assign({}, s, v);
