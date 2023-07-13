package mofokom.slee.testfw.mocks;

import static java.lang.StrictMath.log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.slee.transaction.CommitListener;
import javax.slee.transaction.RollbackListener;
import javax.slee.transaction.SleeTransaction;
import javax.slee.transaction.SleeTransactionManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;

/**
 *
 * @author wozza
 */
//@Slf4j
public class MockSleeTransactionManager extends MockSleeTransaction implements SleeTransactionManager {

    MockSleeTransaction tx;
    static final Logger log = Logger.getLogger(MockSleeTransactionManager.class.getName());

    @Override
    public SleeTransaction beginSleeTransaction() throws NotSupportedException, SystemException {
        return (tx = new MockSleeTransaction());
    }

    @Override
    public SleeTransaction getSleeTransaction() throws SystemException {
        return tx;
    }

    @Override
    public SleeTransaction asSleeTransaction(Transaction t) throws NullPointerException, IllegalArgumentException, SystemException {
        return (SleeTransaction) t;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        beginSleeTransaction();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return tx;
    }

    @Override
    public void resume(Transaction tx) throws InvalidTransactionException, IllegalStateException, SystemException {
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
    }

    @Override
    public Transaction suspend() throws SystemException {
        return tx;
    }

    public boolean isMarkedRollback() throws SystemException {
        return tx.getStatus() == Status.STATUS_MARKED_ROLLBACK;
    }

    public <P, R> void enlist(Function<P, R> f) throws XAException, SystemException, IllegalStateException, RollbackException {
        XAResource xar = Mockito.mock(XAResource.class);
        doAnswer(ic -> {
            return f.apply(null);
        }).when(xar).commit(any(Xid.class), anyBoolean());
        getSleeTransaction().enlistResource(xar);
    }

}

class MockSleeTransaction implements SleeTransaction {

    static final Logger log = Logger.getLogger(MockSleeTransactionManager.class.getName());

    Set<XAResource> xa = new HashSet<>();
    private boolean rollback;

    private int status = Status.STATUS_ACTIVE;

    Xid xid = new Xid() {
        @Override
        public int getFormatId() {
            return 0;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return UUID.randomUUID().toString().getBytes();
        }

        @Override
        public byte[] getBranchQualifier() {
            return UUID.randomUUID().toString().getBytes();
        }
    };

    @Override
    public void asyncCommit(CommitListener cl) throws IllegalStateException, SecurityException {
        try {
            commit();
            cl.committed();
        } catch (RollbackException ex) {
            cl.rolledBack(ex);
        } catch (HeuristicMixedException ex) {
            cl.heuristicMixed(ex);
        } catch (HeuristicRollbackException ex) {
            cl.heuristicRollback(ex);
        } catch (SystemException ex) {
            cl.systemException(ex);
        }
    }

    @Override
    public void asyncRollback(RollbackListener rl) throws IllegalStateException, SecurityException {
        try {
            rollback();
            rl.rolledBack();
        } catch (SystemException ex) {
            rl.systemException(ex);
        }
    }

    @Override
    public boolean enlistResource(XAResource xar) throws IllegalStateException, RollbackException {
        xa.add(xar);
        return true;
    }

    @Override
    public boolean delistResource(XAResource xar, int i) throws IllegalStateException, SystemException {
        xa.remove(xar);
        return true;
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        if (!rollback) {
            updateStatus(Status.STATUS_COMMITTING);
            xa.stream().forEach(x -> {
                try {
                    x.commit(xid, true);
                } catch (XAException ex) {
                    Logger.getLogger(MockSleeTransaction.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            updateStatus(Status.STATUS_COMMITTED);
        } else {
            rollback();
        }
    }

    @Override
    public int getStatus() throws SystemException {
        return status;
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        updateStatus(Status.STATUS_ROLLING_BACK);
        xa.stream().forEach(x -> {
            try {
                x.rollback(xid);
            } catch (XAException ex) {
                Logger.getLogger(MockSleeTransaction.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        updateStatus(Status.STATUS_ROLLEDBACK);
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        this.rollback = true;
        updateStatus(Status.STATUS_MARKED_ROLLBACK);
    }

    private void updateStatus(int i) {
        log.info("txn " + StatusEnum.from(i));
    }

}

enum StatusEnum {
    STATUS_ACTIVE(0),
    STATUS_MARKED_ROLLBACK(1),
    STATUS_PREPARED(2),
    STATUS_COMMITTED(3),
    STATUS_ROLLEDBACK(4),
    STATUS_UNKNOWN(5),
    STATUS_NO_TRANSACTION(6),
    STATUS_PREPARING(7),
    STATUS_COMMITTING(8),
    STATUS_ROLLING_BACK(9);
    private int i;

    StatusEnum(int i) {
        this.i = i;
    }

    public static StatusEnum from(int i) {
        return Arrays.asList(StatusEnum.values()).stream().filter(s -> s.i == i).findFirst().orElseThrow();
    }
}
