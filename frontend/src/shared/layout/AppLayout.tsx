import type { ReactNode } from "react";

import MyAppBar from "./AppBarTop";

const AppLayout = ({ children }: { children: ReactNode }) => (
  <>
    <MyAppBar />
    {children}
  </>
);

export default AppLayout;
