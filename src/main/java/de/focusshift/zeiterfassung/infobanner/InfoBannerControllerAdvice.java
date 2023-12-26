package de.focusshift.zeiterfassung.infobanner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

class InfoBannerControllerAdvice implements HandlerInterceptor {

    private final InfoBannerConfigProperties properties;

    InfoBannerControllerAdvice(InfoBannerConfigProperties properties) {
        this.properties = properties;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {

        if (modelAndView != null && modelAndView.hasView() && !redirectOrForward(modelAndView)) {
            modelAndView.getModelMap().addAttribute("infoBannerText", properties.text().de());
        }
    }

    private static boolean redirectOrForward(ModelAndView modelAndView) {
        final String viewName = modelAndView.getViewName();
        return viewName != null && (viewName.startsWith("redirect") || viewName.startsWith("forward"));
    }
}
