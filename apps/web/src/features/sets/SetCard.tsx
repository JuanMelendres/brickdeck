import Link from "next/link";
import {
  Card,
  CardContent,
  CardMedia,
  Link as MuiLink,
  Stack,
  Typography,
} from "@mui/material";
import type { BrickSetResponse } from "@/lib/types/api";

export function SetCard({ set }: { set: BrickSetResponse }) {
  return (
    <Card variant="outlined" sx={{ height: "100%" }}>
      {set.imageUrl ? (
        <CardMedia
          component="img"
          image={set.imageUrl}
          alt={set.name ?? ""}
          sx={{ height: 160, objectFit: "contain", bgcolor: "grey.100", p: 1 }}
        />
      ) : null}
      <CardContent>
        <Typography variant="subtitle1" component="h3" gutterBottom>
          <MuiLink
            component={Link}
            href={`/sets/${encodeURIComponent(set.externalSetNumber ?? "")}`}
            underline="hover"
            color="inherit"
          >
            {set.name}
          </MuiLink>
        </Typography>
        <Stack spacing={0.5}>
          <Typography variant="body2" color="text.secondary">
            {set.externalSetNumber}
            {set.yearReleased ? ` · ${set.yearReleased}` : ""}
          </Typography>
          {set.themeName ? (
            <Typography variant="body2" color="text.secondary">
              {set.themeName}
            </Typography>
          ) : null}
          {set.numberOfParts ? (
            <Typography variant="body2" color="text.secondary">
              {set.numberOfParts} parts
            </Typography>
          ) : null}
          {set.externalUrl ? (
            <MuiLink
              href={set.externalUrl}
              target="_blank"
              rel="noopener noreferrer"
              variant="body2"
            >
              View on Rebrickable
            </MuiLink>
          ) : null}
        </Stack>
      </CardContent>
    </Card>
  );
}
