package ma.sir.event.service.facade.admin;

import ma.sir.event.bean.core.Salle;
import ma.sir.event.dao.criteria.core.SalleCriteria;
import ma.sir.event.dao.criteria.history.SalleHistoryCriteria;
import ma.sir.event.zynerator.service.IService;

import java.util.List;

public interface SalleAdminService extends IService<Salle, SalleCriteria, SalleHistoryCriteria> {

    List<Salle> findByBlocOperatoirId(Long id);

    int deleteByBlocOperatoirId(Long id);


    Salle finByReference(String reference);
}
