import Card from "@mui/material/Card";
import CardActions from "@mui/material/CardActions";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import type { ReactNode } from "react";

type SectionCardProps = {
  actions?: ReactNode;
  children: ReactNode;
  subtitle: string;
  title: string;
};

const SectionCard = ({ actions, children, subtitle, title }: SectionCardProps) => (
  <Card
    sx={{
      backgroundColor: "background.paper",
      border: "1px solid",
      borderColor: "divider",
      borderRadius: 1,
    }}
  >
    <CardHeader
      title={title}
      subheader={subtitle}
      slotProps={{
        subheader: { sx: { color: "text.secondary", fontSize: 13 } },
        title: { sx: { fontSize: 16, fontWeight: 600 } },
      }}
      sx={{ pb: 0 }}
    />
    <CardContent>{children}</CardContent>
    {actions && (
      <CardActions sx={{ justifyContent: "flex-end", px: 2, pt: 0 }}>
        {actions}
      </CardActions>
    )}
  </Card>
);

export default SectionCard;
