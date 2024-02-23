async function resetPassword(data) {
  const res = await fetch(
    `${import.meta.env.VITE_BACKEND}/api/v1/auth/reset-password`,
    {
      method: "POST",
      mode: "cors",
      cache: "no-cache",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "react-front",
      },
      body: JSON.stringify({
        resetToken: data.token,
        newPassword: data.newPassword,
      }),
    }
  );
  if (!res.ok) {
    const err = await res.json();
    switch (err.title) {
      case "INVALID_TOKEN_ERROR":
        throw new Error("Password reset link is invalid or expired");
      case "PASSWORD_RESET_ERROR":
        throw new Error("New password can not be old password");
      default:
        throw new Error(
          "An error has occured while trying to reset your password"
        );
    }
  }
}

export default resetPassword;