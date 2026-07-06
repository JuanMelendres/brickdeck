import {
  Box,
  Link as MuiLink,
  Stack,
  Typography,
} from "@mui/material";
import type { BrickSetResponse } from "@/lib/types/api";

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <Typography variant="body2" color="text.secondary">
      <Box component="span" sx={{ fontWeight: 600 }}>
        {label}:
      </Box>{" "}
      {value}
    </Typography>
  );
}

export function SetDetail({ set }: { set: BrickSetResponse }) {
  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={3}
      sx={{ alignItems: "flex-start" }}
    >
      {set.imageUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={set.imageUrl}
          alt={set.name ?? ""}
          style={{ width: 200, height: "auto", objectFit: "contain" }}
        />
      ) : null}
      <Stack spacing={1}>
        <Typography variant="h4" component="h1">
          {set.name}
        </Typography>
        <Fact label="Set number" value={set.externalSetNumber ?? "—"} />
        {set.yearReleased ? (
          <Fact label="Year" value={String(set.yearReleased)} />
        ) : null}
        {set.themeName ? <Fact label="Theme" value={set.themeName} /> : null}
        {set.numberOfParts ? (
          <Fact label="Parts" value={String(set.numberOfParts)} />
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
    </Stack>
  );
}
