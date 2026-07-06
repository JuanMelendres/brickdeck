"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Box, Button, TextField } from "@mui/material";

const schema = z.object({
  query: z.string().trim().min(1, "Enter a search term"),
});

type SearchForm = z.infer<typeof schema>;

interface SetSearchBarProps {
  onSearch: (query: string) => void;
  defaultQuery?: string;
}

export function SetSearchBar({ onSearch, defaultQuery = "" }: SetSearchBarProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SearchForm>({
    resolver: zodResolver(schema),
    defaultValues: { query: defaultQuery },
  });

  return (
    <Box
      component="form"
      onSubmit={handleSubmit((data) => onSearch(data.query))}
      sx={{ display: "flex", gap: 2, alignItems: "flex-start" }}
      noValidate
    >
      <TextField
        label="Search sets"
        fullWidth
        {...register("query")}
        error={Boolean(errors.query)}
        helperText={errors.query?.message ?? " "}
      />
      <Button type="submit" variant="contained" size="large" sx={{ mt: 1 }}>
        Search
      </Button>
    </Box>
  );
}
