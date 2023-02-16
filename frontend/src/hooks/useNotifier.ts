import { useSnackbar } from "notistack";
import { useDispatch, useSelector } from "react-redux";
import { NotificationState } from "../redux/model/notify";
import { useEffect } from "react";
import { Dispatch } from "../redux/store";

let displayed: number[] = [];

export const useNotifier = () => {
  const dispatch = useDispatch<Dispatch>();
  const notifications = useSelector(
    (state: { notify: NotificationState }) => state.notify.notifications
  );
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();

  const storeDisplayed = (id: number) => {
    displayed = [...displayed, id];
  };

  const removeDisplayed = (id: number) => {
    displayed = [...displayed.filter((key) => id !== key)];
  };

  useEffect(() => {
    notifications.forEach((notification) => {
      if (displayed.includes(notification.key)) return;
      enqueueSnackbar(notification.message, {
        key: notification.key,
        variant: notification.variant,
        autoHideDuration: 2500,
        onExited: () => {
          dispatch.notify.removeNotificationById(notification);
          removeDisplayed(notification.key);
        },
      });
      dispatch.notify.removeNotificationById(notification);
      storeDisplayed(notification.key);
    });
  }, [notifications, closeSnackbar, enqueueSnackbar, dispatch]);
};
