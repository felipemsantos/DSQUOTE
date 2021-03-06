package org.tmf.dsmapi.quote;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tmf.dsmapi.commons.facade.AbstractFacade;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.tmf.dsmapi.commons.exceptions.BadUsageException;
import org.tmf.dsmapi.commons.exceptions.ExceptionType;
import org.tmf.dsmapi.commons.exceptions.UnknownResourceException;
import org.tmf.dsmapi.commons.utils.BeanUtils;
import org.tmf.dsmapi.quote.model.Quote;
import org.tmf.dsmapi.quote.event.QuoteEventPublisherLocal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.tmf.dsmapi.quote.model.QuoteItem;
import org.tmf.dsmapi.quote.model.StateQuote;
import org.tmf.dsmapi.quote.model.StateQuoteItem;

@Stateless
public class QuoteFacade extends AbstractFacade<Quote> {

    @PersistenceContext(unitName = "DSQuotePU")
    private EntityManager em;
    @EJB
    QuoteEventPublisherLocal publisher;
    StateQuoteImpl stateModel = new StateQuoteImpl();

    public QuoteFacade() {
        super(Quote.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void checkCreation(Quote newQuote) throws BadUsageException, UnknownResourceException {

        Quote quoteEntity = null;
        if (newQuote.getId() == null
                || newQuote.getId().isEmpty()) {
//            throw new BadUsageException(ExceptionType.BAD_USAGE_GENERIC, "While creating Quote, id must be not null");
                //Do nothing create ok
                Logger.getLogger(QuoteFacade.class.getName()).log(Level.INFO, "Quote with autogenerated id is being posted");
        } else {
            try {
                quoteEntity = this.find(newQuote.getId());
                if (null != quoteEntity) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_GENERIC,
                            "Duplicate Exception, Quote with same id :" + newQuote.getId() + " alreay exists");
                }
            } catch (UnknownResourceException ex) {
                //Do nothing create ok
                Logger.getLogger(QuoteFacade.class.getName()).log(Level.INFO, "Quote with id = " + newQuote.getId() + " is being posted", ex);
            }
        }

        //verify first status
        if (null == newQuote.getState()) {
            newQuote.setState(StateQuote.IN_PROGRESS);
//            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "LifecycleState is mandatory");
        } else {
            if (!newQuote.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState " + newQuote.getState().value() + " is not the first state, attempt : " + StateQuote.IN_PROGRESS.value());
            }
        }

        if (null == newQuote.getVersion()
                || newQuote.getVersion().isEmpty()) {
            newQuote.setVersion("1.0");
        }
        
        if (null == newQuote.getCustomer()){
            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "customer is mandatory");
        }

//      POST Mandatory attributes within product Quote item
        if (null != newQuote.getQuoteItem()
                && !newQuote.getQuoteItem().isEmpty()) {
            List<QuoteItem> l_quoteItem = newQuote.getQuoteItem();
            for (QuoteItem quoteItem : l_quoteItem) {                
                if (null == quoteItem.getState()) {
                    quoteItem.setState(StateQuoteItem.IN_PROGRESS);
                } else {
                    if (!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.IN_PROGRESS.name())) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState " + quoteItem.getState().value() + " is not the first state, attempt : " + StateQuote.IN_PROGRESS.value());
                    }
                }
                if (null == quoteItem.getId()
                        || quoteItem.getId().isEmpty()) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.id  is mandatory");
                }
                if (null == quoteItem.getProductOffering()) {
                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.productOffering is mandatory");
                }
                if (null == quoteItem.getAction()
                        || quoteItem.getAction().isEmpty()) {
                    quoteItem.setAction("add");
                } else {
                    if(! quoteItem.getAction().equalsIgnoreCase("add")){
                        throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.action must be 'add' in creation");
                    }
                }
//                if (null == quoteItem.getAction()
//                        || quoteItem.getAction().isEmpty()) {
//                    throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.action is mandatory");
//                }
//                if (quoteItem.getAction().equalsIgnoreCase("add")) {
//                    if (null == quoteItem.getProductOffering()) {
//                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.productOffering is mandatory if action is add");
//                    } else {
//                        if ((null == quoteItem.getProductOffering().getId() || quoteItem.getProductOffering().getId().isEmpty())
//                                && (null == quoteItem.getProductOffering().getHref() || quoteItem.getProductOffering().getHref().isEmpty())) {
//                            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
//                                    "quoteItem.productOffering id AND/OR href are mandatory");
//                        }
//                    }
//                    if (null != quoteItem.getProduct()) {
//                        if ((null == quoteItem.getProduct().getProductCharacteristic()
//                                || quoteItem.getProduct().getProductCharacteristic().isEmpty())
//                                &&
//                                (null == quoteItem.getProduct().getProductRelationship()
//                                || quoteItem.getProduct().getProductRelationship().isEmpty())) {
//                            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.product.productCharacteristic or quoteItem.product.productRelationship are mandatory if action is add");
//                        }
////                        if ((null == quoteItem.getProduct().getProductRelationship()
////                                || quoteItem.getProduct().getProductRelationship().isEmpty())) {
////                            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem.product.productRelationship is mandatory if action is add");
////                        }
//                    }
//                } else if (quoteItem.getAction().equalsIgnoreCase("modify")
//                        || quoteItem.getAction().equalsIgnoreCase("delete")) {
//                    if (null != quoteItem.getProduct()) {
//                        if ((null == quoteItem.getProduct().getId() || quoteItem.getProduct().getId().isEmpty())
//                                && (null == quoteItem.getProduct().getHref() || quoteItem.getProduct().getHref().isEmpty())) {
//                            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
//                                    "quoteItem.product id AND/OR href are mandatory  if action is 'modify' or 'delete'");
//                        }
//                    } else {
//                        throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS,
//                                "quoteItem.product is mandatory if action is 'modify' or 'delete'");
//                    }
//                }
            }
        } else {
            throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "quoteItem is mandatory");
        }

    
    
    }

    public Quote patchAttributs(String id, Quote partialEntity) throws UnknownResourceException, BadUsageException {
        Quote currentEntity = this.find(id);

        if (currentEntity == null) {
            throw new UnknownResourceException(ExceptionType.UNKNOWN_RESOURCE);
        }

        verifyStatus(currentEntity, partialEntity);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.convertValue(partialEntity, JsonNode.class);
        partialEntity.setId(id);
        // appel de la methode pour patcher l'entity
        if (BeanUtils.patch(currentEntity, partialEntity, node)) {
            publisher.valueChangedNotification(currentEntity, new Date());
        }

        return currentEntity;
    }

    public void verifyStatus(Quote currentEntity, Quote partialEntity) throws BadUsageException {
        if (null != partialEntity.getState()) {
            stateModel.checkTransition(currentEntity.getState(), partialEntity.getState());
            publisher.statusChangedNotification(currentEntity, new Date());
        }
    }

    public Quote checkPatchAttributs(long id, Quote partialEntity) throws UnknownResourceException, BadUsageException {
        Quote currentEntity = this.find(id);

        if (currentEntity == null) {
            throw new UnknownResourceException(ExceptionType.UNKNOWN_RESOURCE);
        }

        if (null != partialEntity.getState()) {
            stateModel.checkTransition(currentEntity.getState(), partialEntity.getState());
            System.out.println("About to publish statusChangedNotification ");

            publisher.statusChangedNotification(currentEntity, new Date());
        } else {
            System.out.println("No State detectd ");
            //throw new BadUsageException(ExceptionType.BAD_USAGE_MANDATORY_FIELDS, "state" + " is not found");
        }

        if (null != partialEntity.getId()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "id is not patchable");
        }

        if (null != partialEntity.getHref()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "href is not patchable");
        }

        if (null != partialEntity.getExternalId()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "externalId not patchable");
        }

        if (null != partialEntity.getState()) {
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.IN_PROGRESS);
                        } else {
                            if (!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.IN_PROGRESS.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state InProgress is mandatory with Quote State InProgress");
                            }
                        }
                    }
                }
            }
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.IN_PROGRESS);
                        } else {
                            if ((!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.PENDING.name()))
                                    && !quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.IN_PROGRESS.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state InProgress or Pending is mandatory with Quote State Pending");
                            }
                        }
                    }
                }
            }
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.APPROVED.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.APPROVED);
                        } else {
                            if (!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.APPROVED.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state Approvec is mandatory with Quote State Approvec");
                            }
                        }
                    }
                }
            }
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.CANCELLED.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.IN_PROGRESS);
                        } else {
                            if ((!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.PENDING.name()))
                                    && !quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.IN_PROGRESS.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state InProgress or Pending is mandatory with Quote State Cancelled");
                            }
                        }
                    }
                }
            }
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.ACCEPTED.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.APPROVED);
                        } else {
                            if (!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.APPROVED.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state Approvec is mandatory with Quote State Accepted");
                            }
                        }
                    }
                }
            }
            if (partialEntity.getState().name().equalsIgnoreCase(StateQuote.REJECTED.name())) {
                if (null != currentEntity.getQuoteItem()
                        && !currentEntity.getQuoteItem().isEmpty()) {
                    for (QuoteItem quoteItem : currentEntity.getQuoteItem()) {
                        if (null == quoteItem.getState()) {
                            quoteItem.setState(StateQuoteItem.REJECTED);
                        } else {
                            if ((!quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.REJECTED.name()))
                                    && !quoteItem.getState().name().equalsIgnoreCase(StateQuoteItem.APPROVED.name())) {
                                throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state Rejected or Approved is mandatory with Quote State Rejected");
                            }
                        }
                    }
                }
            }
        }

        if (null != partialEntity.getVersion() || !partialEntity.getVersion().isEmpty()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "version is not patchable");
        }

        if (null != partialEntity.getQuoteDate()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteDate is not patchable");
        }

        if (null != partialEntity.getEffectiveQuoteCompletionDate()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "effectiveQuoteCompletionDate is not patchable");
        }

        if (null != partialEntity.getQuoteCompletionDate()) {
            if( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteCompletionDate is patchable only when quote state is Pending");
            }
        }
        
        if (null != partialEntity.getValidFor()) {
            if( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "ValidFor is patchable only when quote state is Pending");
            }
        }
        
        if (null != partialEntity.getBillingAccount()) {
            if( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "BillingAccount is patchable only when quote state is Pending");
            }
        }
        
        if (null != partialEntity.getNote()) {
            if( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "Note is patchable only when quote state is Pending");
            }
        }
        
        if (null != partialEntity.getCharacteristic()) {
            if( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "Characteristic is patchable only when quote state is Pending");
            }
        }
        
        if (null != partialEntity.getCustomer()) {
            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "Customer is not patchable");
        }
        
        if (null != partialEntity.getRelatedParty()) {
            if( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())) 
                    && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "RelatedParty is patchable only when quote state is Pending or InProgress");
            }
        }
        
        if (null != partialEntity.getAgreement()) {
            if( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())) 
                    && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())){
                throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "Agreement is patchable only when quote state is Pending or InProgress");
            }
        }
        
        if (null != partialEntity.getQuoteItem()) {           
            if (null != partialEntity.getQuoteItem()
                    && !partialEntity.getQuoteItem().isEmpty()) {
                List<QuoteItem> l_quoteItem = partialEntity.getQuoteItem();
                for (QuoteItem quoteItem : l_quoteItem) {
                    if (null != quoteItem.getId()) {
                        throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.id not patchable");
                    }
                    if (quoteItem.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())) {
                        if ( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state InProgress only with Quote State Pending");
                        }
                    }
                    if (quoteItem.getState().name().equalsIgnoreCase(StateQuote.REJECTED.name())) {
                        if ( ! partialEntity.getState().name().equalsIgnoreCase(StateQuote.REJECTED.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_FLOW_TRANSITION, "lifecycleState QuoteItem state Rejected only with Quote State Rejected");
                        }
                    }
                    if (null != quoteItem.getAttachment()) {
                        if ( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name()))
                                && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.attachment not patchable when Quote state is Pending or InProgress");
                        }
                    }
                    if (null != quoteItem.getRelatedParty()) {
                        if ( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name()))
                                && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.relatedParty not patchable when Quote state is Pending or InProgress");
                        }
                    }
                    if (null != quoteItem.getProductOffering()) {
                        if ( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name()))
                                && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.productOffering not patchable when Quote state is Pending or InProgress");
                        }
                    }
                    if (null != quoteItem.getNote()) {
                        if ( (! partialEntity.getState().name().equalsIgnoreCase(StateQuote.PENDING.name()))
                                && partialEntity.getState().name().equalsIgnoreCase(StateQuote.IN_PROGRESS.name())) {
                            throw new BadUsageException(ExceptionType.BAD_USAGE_OPERATOR, "quoteItem.note not patchable when Quote state is Pending or InProgress");
                        }
                    }
                }
            }
        }

        return currentEntity;
    }
}
