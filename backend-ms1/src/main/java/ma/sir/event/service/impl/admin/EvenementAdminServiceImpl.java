package ma.sir.event.service.impl.admin;

import com.corundumstudio.socketio.SocketIOServer;
import ma.sir.event.bean.core.Evenement;
import ma.sir.event.bean.core.EvenementRedis;
import ma.sir.event.bean.core.Salle;
import ma.sir.event.bean.history.EvenementHistory;
import ma.sir.event.dao.criteria.core.EvenementCriteria;
import ma.sir.event.dao.criteria.history.EvenementHistoryCriteria;
import ma.sir.event.dao.facade.core.EvenementDao;
import ma.sir.event.dao.facade.history.EvenementHistoryDao;
import ma.sir.event.dao.specification.core.EvenementSpecification;
import ma.sir.event.service.facade.admin.EvenementAdminService;
import ma.sir.event.service.facade.admin.EvenementStateAdminService;
import ma.sir.event.service.facade.admin.SalleAdminService;
import ma.sir.event.ws.converter.EvenementRedisConverter;
import ma.sir.event.zynerator.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvenementAdminServiceImpl extends AbstractServiceImpl<Evenement, EvenementHistory, EvenementCriteria, EvenementHistoryCriteria, EvenementDao,
        EvenementHistoryDao> implements EvenementAdminService {


    private final SocketIOServer socketIOServer;

    @Override
    public Evenement createAndSendRedis(Evenement evenement) {
        Evenement myEvenement = create(evenement);
        EvenementRedis evenementRedis = evenementRedisConverter.toDto(myEvenement);
        evenementAdminRedisService.save(evenementRedis);
        return myEvenement;
    }

    @Override
    public Evenement create(Evenement evenement) {
        if (evenement.getSalle() != null) {
            Salle salle = salleService.finByReference(evenement.getSalle().getReference());
            evenement.setSalle(salle);
        }
        Evenement myEvenement = super.create(evenement);

        return myEvenement;
    }

    public Evenement findByReferenceEntity(Evenement t) {
        return dao.findByReference(t.getReference());
    }


    // @Cacheable(cacheNames = "itemCache", key = "#id", cacheManager = "redisCacheManager")
    public List<Evenement> findBySalleId(Long id) {
        return dao.findBySalleId(id);
    }

    public int deleteBySalleId(Long id) {
        return dao.deleteBySalleId(id);
    }

    public List<Evenement> findByEvenementStateId(Long id) {
        return dao.findByEvenementStateId(id);
    }

    public int deleteByEvenementStateId(Long id) {
        return dao.deleteByEvenementStateId(id);
    }

    @Override
    public Evenement findByReference(String reference) {
        return dao.findByReference(reference);
    }

    public void configure() {
        super.configure(Evenement.class, EvenementHistory.class, EvenementHistoryCriteria.class, EvenementSpecification.class);
    }

    @Autowired
    private EvenementStateAdminService evenementStateService;
    @Autowired
    private SalleAdminService salleService;
    @Autowired
    private EvenementAdminRedisServiceImpl evenementAdminRedisService;
    @Autowired
    private EvenementRedisConverter evenementRedisConverter;


    public EvenementAdminServiceImpl(EvenementDao dao, EvenementHistoryDao historyDao, SocketIOServer socketIOServer) {
        super(dao, historyDao);
        this.socketIOServer = socketIOServer;
    }

}