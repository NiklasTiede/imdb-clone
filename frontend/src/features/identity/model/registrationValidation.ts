import * as zod from "zod";
import { i18n } from "../../../i18n";

export interface RegistrationFormInputs {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export const passwordRules = [
  {
    label: "8-30 characters",
    met: (password: string) => password.length >= 8 && password.length <= 30,
  },
  {
    label: "One uppercase letter",
    met: (password: string) => /[A-Z]/.test(password),
  },
  {
    label: "One lowercase letter",
    met: (password: string) => /[a-z]/.test(password),
  },
  { label: "One number", met: (password: string) => /[0-9]/.test(password) },
  {
    label: "One special character",
    met: (password: string) => /[#?!@$%^&*-]/.test(password),
  },
] as const;

export const registrationSchema = zod
  .object({
    username: zod
      .string()
      .min(2, "Username must contain at least 2 characters")
      .max(20, "Username must contain at most 20 characters")
      .regex(
        new RegExp(i18n.regex.username.pattern),
        i18n.regex.username.rules,
      ),
    email: zod
      .string()
      .regex(new RegExp(i18n.regex.email.pattern), i18n.regex.email.rules),
    password: zod
      .string()
      .min(8, "Password must contain at least 8 characters")
      .max(30, "Password must contain at most 30 characters")
      .regex(
        new RegExp(i18n.regex.password.pattern),
        i18n.regex.password.rules,
      ),
    confirmPassword: zod.string().min(1, "Confirm your password"),
  })
  .superRefine(({ confirmPassword, password }, ctx) => {
    if (confirmPassword !== password) {
      ctx.addIssue({
        path: ["confirmPassword"],
        code: "custom",
        message: "Passwords do not match",
      });
    }
  });
