package mofokom.slee.testfw;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import javax.naming.*;
import javax.naming.spi.InitialContextFactory;

public class NewInitialContextFactory implements InitialContextFactory {

    private static final Logger logger = Logger.getLogger(NewInitialContextFactory.class.getSimpleName());

    private static class NewContext implements Context {

        static Map<String, Object> map = new HashMap<String, Object>();
        NewContext env;

        public NewContext() {
        }

        public Object lookup(Name name) throws NamingException {
            return lookup(name.toString());
        }

        public Object lookup(String name) throws NamingException {
            if (env == null) {
                env = new NewContext();
                map.put("java:comp/env", env);
            }
            logger.info("lookup " + name);
            if (!map.containsKey(name)) {
                throw new NamingException(name + " not found");
            }
            return map.get(name);
        }

        public void bind(Name name, Object obj) throws NamingException {
            bind(name.toString(), obj);
        }

        public void bind(String name, Object obj) throws NamingException {
            System.err.println("bind " + name + " " + obj);
            if (env == null) {
                env = new NewContext();
                map.put("java:comp/env", env);
            }
            logger.info("bind " + name + " " + obj);
            map.put(name, obj);
        }

        public void rebind(Name name, Object obj) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void rebind(String name, Object obj) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void unbind(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void unbind(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void rename(Name oldName, Name newName) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void rename(String oldName, String newName) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void destroySubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void destroySubcontext(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Context createSubcontext(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Context createSubcontext(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object lookupLink(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object lookupLink(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NameParser getNameParser(Name name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public NameParser getNameParser(String name) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Name composeName(Name name, Name prefix) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String composeName(String name, String prefix) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object removeFromEnvironment(String propName) throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Hashtable<?, ?> getEnvironment() throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getNameInNamespace() throws NamingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    private Context context = null;

    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        if (context == null) {
            context = new NewContext();
        }
        return context;
    }
}
