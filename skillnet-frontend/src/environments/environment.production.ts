/**
 * Producción (Vercel) — API vía proxy reverso en el edge (/api → EC2 Ohio).
 */
export const environment = {
  production: true,
  apiUrl: '/api/v1',
  googleClientId: '226869771343-duthig8qnupr5j7u1l61bkl95hmc284n.apps.googleusercontent.com',
  googleSignInEnabled: true,
  /** Sustituir por pk_live_... cuando Stripe esté en modo live. */
  stripePublicKey:
    'pk_test_51TSsnbCnpuQpOjHLcGfpme3tI5iLN6vZk5vioX3FwJtfQJVT05mNKcLUuv036NpSQEWJ9Ndg7uTiGGGpI5uhqB7S001xpLylpM',
  stripeDevMock: false,
};
