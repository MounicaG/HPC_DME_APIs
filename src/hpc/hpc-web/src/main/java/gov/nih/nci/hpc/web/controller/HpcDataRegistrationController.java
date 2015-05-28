package gov.nih.nci.hpc.web.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcDatasetLocation;
import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.dto.HpcDataRegistrationOutput;
import gov.nih.nci.hpc.web.model.HpcRegistration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

@RestController
@EnableAutoConfiguration
@RequestMapping("/register")
public class HpcDataRegistrationController extends AbstractHpcController {
 
	  
  @RequestMapping(method = RequestMethod.GET)
  public List<HpcRegistration> findDatasets(){ 
    return null;
  }
	
  @RequestMapping(method = RequestMethod.POST)
  public String register(@RequestBody  HpcRegistration registration, Model model) {
	  RestTemplate restTemplate = new RestTemplate();
	  String uri = "http://localhost:7737/hpc-server/registration";
	  HpcDataRegistrationInput input = new HpcDataRegistrationInput();
	  input.setInvestigatorName(registration.getInvestigatorName());
	  input.setProjectName(registration.getProjectName());
	  HpcDataset dataset = new HpcDataset();
	  HpcDatasetLocation source = new HpcDatasetLocation();
	  source.setEndpoint(registration.getOriginDataendpoint());
	  source.setFilePath(registration.getOriginDataLocation());
	  dataset.setSource(source);
	  List<HpcDataset> sets = input.getDatasets();
	  sets.add(dataset);
	  try
	  {
		  String result = (String) restTemplate.postForObject( uri, input, String.class);
		  model.addAttribute("registrationOutput", result);
	  }
	  catch(Exception e)
	  {
		  model.addAttribute("registrationOutput", "Failed to register your request due to: "+e.getMessage());
	  }
	  return "result.html";
  }
}
