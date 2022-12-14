package com.labcomu.ha.prototype.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.labcomu.ha.prototype.model.Book;
import com.labcomu.ha.prototype.model.BookScore;


@RestController
@RequestMapping("api/book")
@Validated
public class BookStoreController {
	
	private BookScoreGateway bookScoreGateway;
	
	private List<Book> bookInventory;
	
	Logger logger = LoggerFactory.getLogger(BookStoreController.class);
	
	public BookStoreController() {
		
		bookInventory = new ArrayList<Book>();
		Random randomYearGenerator = new Random();
		
		for(int i = 1; i<=10000;i++) {
			Book book = new Book("Book "+ i,"Prentice Hall PTR",randomYearGenerator.nextInt(1990,2022));
			bookInventory.add(book);
		}
		
		bookScoreGateway = BookScoreGateway.getInstance();
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Book> findBook(@PathVariable int id, @RequestParam(name = "cb", required = false) Boolean cbEnabled) {

		ResponseEntity<Book> response;
		
		boolean useCircuitBreaker = (cbEnabled !=null? cbEnabled.booleanValue():false);
		try {

			response = getBookResponse(id, useCircuitBreaker);
	
			
		} catch (Exception e) {
			response = new ResponseEntity<Book>(HttpStatus.SERVICE_UNAVAILABLE);
			logger.info("Response ("+id+") SERVICE_UNAVAILABLE ["+e.getMessage()+"]");


		}

		return response;
	}

	private ResponseEntity<Book> getBookResponse(int id, boolean cbEnabled) {
		int index = 0;
		Book book;
		ResponseEntity<Book> response;
		HttpStatus status = HttpStatus.OK;

		logger.info("Calling GetBook endpoint by id="+id);
		
		
		if(id>0 && id <= bookInventory.size()) {
			index = id -1;
			book = bookInventory.get(index);
			
			BookScore bookScore;
			
			if(cbEnabled) {
				bookScore = bookScoreGateway.getBookScoreCB(id);
				
				if(bookScore.equals(BookScore.EMPTY)) {
					status = HttpStatus.PARTIAL_CONTENT;
				}
			}
			else {
				bookScore = bookScoreGateway.getBookScore(id);

			}
			
			book.setScore(bookScore);
		}
		else {
			
			book = Book.NULLABLE_BOOK;
			status = HttpStatus.NO_CONTENT;
		}
		
		
		response = new ResponseEntity<Book>(book, status);


		logger.info("Returning Book="+book);
		logger.info("Status Response="+status);
		return response;
	}


	
	public ResponseEntity<Book> getBookFallback(int id, Exception e) {
		
		logger.info("Fallback for GetBook endpoint call");
		
		
		return new ResponseEntity<Book>(Book.NULLABLE_BOOK,HttpStatus.NO_CONTENT);
	}
	
	


	@GetMapping("/")
	public List<Book> getAllBook() {
		
		return bookInventory;
	}
	
}
