import Link from "next/link";
import { Button, Container, Stack, Typography } from "@mui/material";

export default function HomePage() {
  return (
    <Container maxWidth="md" sx={{ py: 8 }}>
      <Stack spacing={3} sx={{ alignItems: "flex-start" }}>
        <Typography variant="h3" component="h1">
          BrickDeck
        </Typography>
        <Typography color="text.secondary">
          Search the LEGO catalog and explore set inventories.
        </Typography>
        <Link href="/sets" style={{ textDecoration: "none" }}>
          <Button variant="contained" size="large">
            Search sets
          </Button>
        </Link>
      </Stack>
    </Container>
  );
}
