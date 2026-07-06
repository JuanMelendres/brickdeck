"use client";

import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#b71c1c" },
    secondary: { main: "#ffca28" },
  },
  shape: { borderRadius: 8 },
});
