/*
 * Copyright (c) 2010-2013, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knopflerfish.bundle.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentConstants;


/**
 *
 */
class ReferenceListener implements ServiceListener
{
  private static final int NO_OP = 0;
  private static final int ADD_OP = 1;
  private static final int DELETE_OP = 2;
  
  final Reference ref;
  private final HashMap /* ServiceReference -> Object */ bound = new HashMap();
  private final HashSet /* ServiceReference */ serviceRefs = new HashSet();
  private HashSet /* ServiceReference */ unbinding = new HashSet();
  private ServiceReference selectedServiceRef = null;
  private final TreeSet /* String */ pids = new TreeSet();
  private Filter cmTarget;
  private final LinkedList /* ServiceEvent */ sEventQueue = new LinkedList();
  private HashMap /* ServiceReference -> HashSet(ComponentContextImpl) */ cciBound =
      new HashMap();


  /**
   * Create a listener for services fulfilling this reference
   * with specified CM configuration.
   *
   */
  ReferenceListener(Reference ref, Configuration config) {
    this.ref = ref;
    setTarget(config);
  }


  /**
   *
   */
  public String toString() {
    return "ReferenceListener(" + ref + ", target=" + cmTarget + ")";
  }


  /**
   * Stop listening for services fulfilling this reference
   *
   */
  void stop() {
    ref.comp.bc.removeServiceListener(this);
    Activator.logDebug("Stop listening, ref=" + getName());
  }


  /**
   * Start listening for this reference. It is a bit tricky to
   * get initial state synchronized with the listener.
   *
   */
  void setTarget(Configuration config) {
    if (config != null) {
      cmTarget = ref.getTarget(config.getProperties(), "CM pid = " + config.getPid());
    } else {
      cmTarget = null;
    }
    final String filter = getFilter();
    Activator.logDebug("Start listening, ref=" + getName() + " with filter=" + filter);
    ref.comp.bc.removeServiceListener(this);
    synchronized (serviceRefs) {
      final HashSet oldServiceRefs = (HashSet)serviceRefs.clone();
      serviceRefs.clear();
      ServiceReference [] srs;
      try {
        ref.comp.bc.addServiceListener(this, filter);
        srs = ref.comp.bc.getServiceReferences((String)null, filter);
      } catch (final InvalidSyntaxException ise) {
        throw new RuntimeException("Should not occur, Filter already checked");
      }
      if (srs != null) {
        ServiceReference best = null;
        for (int i = 0; i < srs.length; i++) {
          if (oldServiceRefs.remove(srs[i])) {
            serviceRefs.add(srs[i]);
          } else {
            final ServiceEvent e = new ServiceEvent(ServiceEvent.REGISTERED, srs[i]);
            if (best == null || best.compareTo(srs[i]) < 0) {
              best = srs[i];
              sEventQueue.addFirst(e);
            } else {
              sEventQueue.addLast(e);
            }
          }
        }
      }
      addPid(config != null ? config.getPid() : Component.NO_PID, true);
      for (Iterator sri = oldServiceRefs.iterator(); sri.hasNext(); ) {
        sEventQueue.addLast(new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH,
                                             (ServiceReference)sri.next()));
      }
    }
    serviceChanged(null);
  }


  /**
   *
   */
  boolean checkTargetChanged(Configuration config) {
    final Filter f = ref.getTarget(config.getProperties(), "CM pid = " + config.getPid());
    if (f != null) {
      return !f.equals(cmTarget);
    } else {
      return cmTarget != null;
    }
  }


  /**
   * Add CM pid that is connected to this reference listener.
   *
   * @param pid String with CM pid.
   * @param clear Clear all previous pids connected to this reference listener.
   */
  void addPid(String pid, boolean clear) {
    if (clear) {
      pids.clear();
    }
    pids.add(pid);
  }


  /**
   *
   */
  void removePid(String pid) {
    pids.remove(pid);
  }


  /**
   *
   */
  boolean noPids() {
    return pids.isEmpty();
  }


  /**
   *
   */
  Iterator getPids() {
    return pids != null ? pids.iterator() : null;
  }


  /**
   *
   */
  boolean isOnlyPid(String pid) {
    return pids == null || (pids.size() == 1 && pids.contains(pid));
  }


  /**
   * Is there a service available for this reference?
   */
  boolean isAvailable() {
    return !serviceRefs.isEmpty();
  }


  /**
   * Is this reference dynamic;
   */
  boolean isDynamic() {
    return ref.refDesc.dynamic;
  }


  /**
   * Is this reference of multiple cardinality;
   */
  boolean isMultiple() {
    return ref.refDesc.multiple;
  }


  /**
   * Is this reference optional;
   */
  boolean isOptional() {
    return ref.refDesc.optional;
  }


  /**
   * Get CM filter.
   */
  Filter getTargetFilter() {
    return (cmTarget != null) ? cmTarget : ref.targetFilter;
  }


  /**
   * Get name of reference.
   */
  String getName() {
    return ref.refDesc.name;
  }


  /**
   * Get highest ranked service reference.
   */
  ServiceReference getServiceReference() {
    synchronized (serviceRefs) {
      if (selectedServiceRef == null) {
        setSelected(getHighestRankedServiceReference());
      }
    }
    return selectedServiceRef;
  }


  private ServiceReference getHighestRankedServiceReference() {
    ServiceReference res = null;
    synchronized (serviceRefs) {
      for (Iterator i = serviceRefs.iterator(); i.hasNext(); ) {
        ServiceReference tst = (ServiceReference)i.next();
        if (res == null || res.compareTo(tst) < 0) {
          res = tst;
        }
      }
    }
    return res;
  }


  void setSelected(ServiceReference s) {
    synchronized (serviceRefs) {
      selectedServiceRef = s;
    }
  }


  /**
   * Get highest ranked service.
   */
  Object getService(ComponentContextImpl cci) {
    final ServiceReference sr = getServiceReference();
    if (sr != null) {
      return getServiceCheckActivate(sr, cci);
    }
    return null;
  }


  /**
   * Get service if it belongs to this reference.
   */
  Object getService(ServiceReference sr, ComponentContextImpl cci) {
    boolean c;
    synchronized (serviceRefs) {
      c = serviceRefs.contains(sr) || unbinding.contains(sr);
    }
    Activator.logDebug("RL.getService " + Activator.srInfo(sr) + " available: " + c);
    return c ? getServiceCheckActivate(sr, cci) : null;
  }


  ServiceReference [] getServiceReferences() {
    synchronized (serviceRefs) {
      return (ServiceReference[])serviceRefs.toArray(new ServiceReference[serviceRefs.size()]);
    }
  }


  ServiceReference [] getBoundServiceReferences() {
    synchronized (bound) {
      Set srs = bound.keySet();
      return srs.isEmpty() ? null :
        (ServiceReference [])srs.toArray(new ServiceReference[bound.size()]);
    }
  }


  ArrayList getBoundServiceReferences(ComponentContextImpl cci) {
    ArrayList res = new ArrayList();
    synchronized (bound) {
      for (Iterator i = cciBound.entrySet().iterator(); i.hasNext(); ) {
        Entry e = (Entry)i.next();
        if (((HashSet)e.getValue()).contains(cci)) {
          res.add(e.getKey());
        }
      }
    }
    return res;
  }


  void bound(ServiceReference sr, Object s, ComponentContextImpl cci) {
    synchronized (bound) {
      HashSet ccis = (HashSet)cciBound.get(sr);
      if (ccis == null) {
        ccis = new HashSet();
        cciBound.put(sr, ccis);
        bound.put(sr, s);
      } else if (s != null && bound.get(sr) == null) {
        bound.put(sr, s);
      }
      ccis.add(cci);
    }
  }


  Object getBound(ServiceReference sr) {
    synchronized (bound) {
      return bound.get(sr);
    }
  }


  boolean doUnbound(ServiceReference sr, ComponentContextImpl cci) {
    synchronized (bound) {
      HashSet ccis = (HashSet)cciBound.get(sr);
      return ccis != null && ccis.contains(cci);
    }
  }


  void unbound(ServiceReference sr, ComponentContextImpl cci) {
    Object s = null;
    synchronized (bound) {
      HashSet ccis = (HashSet)cciBound.get(sr);
      if (ccis != null) {
        ccis.remove(cci);
        if  (ccis.isEmpty()) {
          cciBound.remove(sr);
          s = bound.remove(sr);
        }
      }
    }
    if (s != null) {
      ref.comp.bc.ungetService(sr);
    }
  }


  /**
   * Get all services.
   */
  Object[] getServices(ComponentContextImpl cci)
  {
    final ServiceReference [] srs = getServiceReferences();
    final ArrayList res = new ArrayList(srs.length);
    for (int i = 0; i < srs.length; i++) {
      res.add(getServiceCheckActivate(srs[i], cci));
    }
    return res.toArray();
  }

  //
  // ServiceListener
  //

  /**
   *
   */
  public void serviceChanged(ServiceEvent se) {
    ref.comp.scr.postponeCheckin();
    try {
      do {
        boolean wasSelected = false;
        ServiceReference s;
        int op = NO_OP;
        int sr_size = 0;
        synchronized (serviceRefs) {
          if (sEventQueue.isEmpty()) {
            if (se == null) {
              return;
            }
          } else {
            if (se != null) {
              sEventQueue.addLast(se);
            }
            se = (ServiceEvent)sEventQueue.removeFirst();
          }
          s = se.getServiceReference();
          switch (se.getType()) {
          case ServiceEvent.MODIFIED:
          case ServiceEvent.REGISTERED:
            if (serviceRefs.add(s)) {
              op = ADD_OP;
              sr_size = serviceRefs.size();
            }
            break;
          case ServiceEvent.MODIFIED_ENDMATCH:
          case ServiceEvent.UNREGISTERING:
            serviceRefs.remove(s);
            if  (selectedServiceRef == s) {
              wasSelected = true;
            }
            unbinding.add(s);
            op = DELETE_OP;
            sr_size = serviceRefs.size();
            break;
          default:
            // To keep compiler happy
            throw new RuntimeException("Internal error");
          }
        }
        switch (op) {
        case ADD_OP:
          // TODO check should we always call when != 1?
          if (sr_size != 1 || !ref.refAvailable()) {
            refAdded(s);
          }
          break;
        case DELETE_OP:
          boolean deleted = false;
          if (sr_size != 0 || !ref.refUnavailable()) {
            if (ref.isMultiple() || wasSelected) {
              refDeleted(s);
              deleted = true;
            }
          }
          synchronized (serviceRefs) {
            unbinding.remove(s);
            if (!deleted) {
              setSelected(null);
            }
          }
          break;
        }
        se = null;
      } while (!sEventQueue.isEmpty());
    } finally {
      ref.comp.scr.postponeCheckout();
    }
  }

  //
  // Private methods
  //

  /**
   * Get service, but activate before so that we will
   * get any ComponentExceptions.
   */
  private Object getServiceCheckActivate(ServiceReference sr, ComponentContextImpl cci) {
    Object s = getBound(sr);
    if (s == null) {
      final Object o = sr.getProperty(ComponentConstants.COMPONENT_NAME);
      if (o != null && o instanceof String) {
        final Component [] cs = ref.comp.scr.getComponent((String)o);
        if (cs != null) {
          ref.comp.scr.postponeCheckin();
          try {
            for (int i = 0; i < cs.length; i++) {
              final ComponentConfiguration cc = cs[i].getComponentConfiguration(sr);
              if (cc != null) {
                cc.activate(ref.comp.bc.getBundle(), false);
                break;
              }
            }
          } finally {
            ref.comp.scr.postponeCheckout();
          }
        }
      }
      s = ref.comp.bc.getService(sr);
    }
    bound(sr, s, cci);
    return s;
  }


  /**
   * Get filter string for finding this reference.
   */
  private String getFilter() {
    final Filter target = getTargetFilter();
    if (target != null) {
      return "(&(" + Constants.OBJECTCLASS + "=" +
        ref.refDesc.interfaceName +")" + target.toString() + ")";
    } else {
      return "(" + Constants.OBJECTCLASS + "=" + ref.refDesc.interfaceName +")";
    }
  }

  
  /**
   * The tracked reference has been added.
   *
   */
  private void refAdded(ServiceReference s)
  {
    Activator.logDebug("refAdded, " + toString() + ", " + s);
    ArrayList ccs = getComponentConfigs();
    if (isDynamic()) {
      if (isMultiple()) {
        bindReference(s, ccs);
      } else {
        final ServiceReference selected = getServiceReference();
        if (s == selected) {
          bindReference(s, ccs);
        }
      }
    }
  }

  /**
   * The tracked reference has been deleted.
   *
   */
  private void refDeleted(ServiceReference s)
  {
    Activator.logDebug("refDeleted, " + toString() + ", " + s);
    ArrayList ccs = getComponentConfigs();
    if (isDynamic()) {
      setSelected(null);
      if (isMultiple()) {
        unbindReference(s, ccs);
      } else {
        final ServiceReference newS = getServiceReference();
        if (newS != null) {
          bindReference(newS, ccs);
        }
        unbindReference(s, ccs);
      }
    } else {
      // If it is a service we use, check what to do
      reactivate(ccs);
      // Hold on to service during deactivation
      for (Iterator i = ccs.iterator(); i.hasNext(); ) {
        ((ComponentConfiguration) i.next()).waitForDeactivate();
      }
    }
  }

  private void bindReference(ServiceReference s, ArrayList ccs) {
    for (Iterator i = ccs.iterator(); i.hasNext(); ) {
      ((ComponentConfiguration)i.next()).bindReference(this, s);
    }
  }


  private void unbindReference(ServiceReference s, ArrayList ccs) {
    for (Iterator i = ccs.iterator(); i.hasNext(); ) {
      ((ComponentConfiguration)i.next()).unbindReference(this, s);
    }
  }


  private void reactivate(ArrayList ccs) {
    for (Iterator i = ccs.iterator(); i.hasNext(); ) {
      final ComponentConfiguration cc = (ComponentConfiguration)i.next();
      cc.dispose(ComponentConstants.DEACTIVATION_REASON_REFERENCE, false);
    }
    setSelected(null);
    for (Iterator i = ccs.iterator(); i.hasNext(); ) {
      final ComponentConfiguration cc = (ComponentConfiguration)i.next();
      cc.remove(ComponentConstants.DEACTIVATION_REASON_REFERENCE);
    }
  }


    /**
   * Call refUpdated for component configurations that has bound this reference.
   */
  private ArrayList getComponentConfigs() {
    ArrayList res = new ArrayList();
    for (Iterator i = getPids(); i.hasNext(); ) {
      final ComponentConfiguration [] ccs =
        (ComponentConfiguration [])ref.comp.compConfigs.get(i.next());
      if (ccs != null) {
        for (int j = 0; j < ccs.length; j++) {
          res.add(ccs[j]);
        }
      }
    }
    return res;
  }

}
