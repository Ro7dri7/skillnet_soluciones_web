export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  /**
   * Google Cloud → Credentials → OAuth client → Authorized JavaScript origins:
   * http://localhost:4200 y http://127.0.0.1:4200 (puerto por defecto de ng serve).
   */
  googleClientId: '226869771343-duthig8qnupr5j7u1l61bkl95hmc284n.apps.googleusercontent.com',
  googleSignInEnabled: true,
  stripePublicKey:
    'pk_test_51TSsnbCnpuQpOjHLcGfpme3tI5iLN6vZk5vioX3FwJtfQJVT05mNKcLUuv036NpSQEWJ9Ndg7uTiGGGpI5uhqB7S001xpLylpM',
  /** Sin STRIPE_SECRET_KEY en backend: checkout envía token simulado y el API crea matrícula igual. */
  stripeDevMock: true,
};
