import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/** Solo adjunta JWT a peticiones hacia nuestra API (evita enviar token a terceros). */
function isApiRequest(url: string): boolean {
  const apiBase = environment.apiUrl.replace(/\/$/, '');
  if (url.startsWith(apiBase)) {
    return true;
  }
  if (url.startsWith('/api/')) {
    return true;
  }
  return false;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiRequest(req.url)) {
    return next(req);
  }

  const authService = inject(AuthService);
  const token = authService.getToken() ?? localStorage.getItem('skillnet_token');

  if (!token) {
    return next(req);
  }

  if (req.headers.has('Authorization')) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(authReq);
};
