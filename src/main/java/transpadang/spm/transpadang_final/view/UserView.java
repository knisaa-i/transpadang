package transpadang.spm.transpadang_final.view;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import transpadang.spm.transpadang_final.entity.User;

/**
 * Blazebit entity view untuk User.
 * Memproyeksikan kolom yang aman ditampilkan (tanpa password).
 */
@EntityView(User.class)
public interface UserView {

    @IdMapping
    Long getId();

    String getUsername();

    String getNama();

    String getJabatan();

    String getRole();

    Boolean getAktif();
}
