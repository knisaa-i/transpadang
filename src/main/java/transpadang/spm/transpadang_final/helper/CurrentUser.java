package transpadang.spm.transpadang_final.helper;

import com.blazebit.persistence.CriteriaBuilderFactory;
import transpadang.spm.transpadang_final.bean.JWTUserDetail;
import transpadang.spm.transpadang_final.entity.QUser;
import transpadang.spm.transpadang_final.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUser {
    private final EntityManager em;
    private final CriteriaBuilderFactory configBuilder;

    public User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        var principal = auth.getPrincipal();
        if (principal instanceof JWTUserDetail userDetails) {
            return em.find(User.class, userDetails.getUserId());
        }

        if (principal instanceof UserDetails userDetails) {
            var qUser = new QUser("qUser");
            try {
                return configBuilder.create(em, User.class)
                        .from(User.class, qUser.getMetadata().getName())
                        .where(qUser.username.toString()).eq(userDetails.getUsername())
                        .getSingleResult();
            } catch (NoResultException ex) {
                return null;
            }
        }

        return null;
    }
}