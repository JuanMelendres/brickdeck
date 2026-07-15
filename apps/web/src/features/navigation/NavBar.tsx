"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { AppBar, Box, Button, Toolbar, Typography } from "@mui/material";
import { useAuth } from "@/features/auth/useAuth";

export function NavBar() {
  const { status, user, logout } = useAuth();
  const router = useRouter();

  const handleLogout = () => {
    logout();
    router.push("/");
  };

  return (
    <AppBar position="static" color="default" elevation={1}>
      <Toolbar sx={{ gap: 2 }}>
        <Typography
          variant="h6"
          component={Link}
          href="/"
          sx={{ textDecoration: "none", color: "inherit", fontWeight: 700 }}
        >
          BrickDeck
        </Typography>
        <Button component={Link} href="/sets" color="inherit">
          Sets
        </Button>
        <Button component={Link} href="/compare" color="inherit">
          Compare
        </Button>

        <Box sx={{ flexGrow: 1 }} />

        {status === "authenticated" && (
          <>
            <Button component={Link} href="/collection" color="inherit">
              Collection
            </Button>
            <Button component={Link} href="/recommendations" color="inherit">
              Recommendations
            </Button>
            <Typography variant="body2" color="text.secondary">
              {user?.displayName || user?.email}
            </Typography>
            <Button color="inherit" onClick={handleLogout}>
              Log out
            </Button>
          </>
        )}
        {status === "unauthenticated" && (
          <>
            <Button component={Link} href="/login" color="inherit">
              Log in
            </Button>
            <Button component={Link} href="/register" variant="contained">
              Sign up
            </Button>
          </>
        )}
      </Toolbar>
    </AppBar>
  );
}
