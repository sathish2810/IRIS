package com.temenos.interaction.translate.mapper;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.loader.ResourceStateLoader.ResourceStateResult;

public class TestResourceStateMapper {

	private ResourceStateMapper mapper;
	
	private Map<String, Set<String>> statesByPath, methodsByStateName;
	private Map<String, String> statesByRequest, pathsByState;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMap() {
		//given
		ResourceStateResult resourceStateResult = mock(ResourceStateResult.class);
		when(resourceStateResult.getMethods()).thenReturn(new String[]{"GET", "POST"});
		when(resourceStateResult.getPath()).thenReturn("/alpha");
		when(resourceStateResult.getResourceState()).thenReturn(mock(ResourceState.class));
		when(resourceStateResult.getResourceStateId()).thenReturn("Alpha");
		statesByPath = spy(new HashMap<String, Set<String>>());
		methodsByStateName = spy(new HashMap<String, Set<String>>());
		statesByRequest = spy(new HashMap<String, String>());
		pathsByState = spy(new HashMap<String, String>());
		mapper = new ResourceStateMapper(statesByPath, methodsByStateName, 
				statesByRequest, pathsByState);
		//when
		this.mapper.map(resourceStateResult);
		//then
		verify(statesByPath).put(eq("/alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"Alpha"}))));
		verify(methodsByStateName).put(eq("Alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"GET", "POST"}))));
		verify(statesByRequest).put(eq("GET /alpha"), eq("Alpha"));
		verify(statesByRequest).put(eq("POST /alpha"), eq("Alpha"));
		verify(pathsByState).put(eq("Alpha"), eq("/alpha"));
	}
	
	@Test
	public void testMapWithNonNullMethods(){
		//given
		ResourceStateResult resourceStateResult = mock(ResourceStateResult.class);
		when(resourceStateResult.getMethods()).thenReturn(new String[]{"GET", "POST"});
		when(resourceStateResult.getPath()).thenReturn("/alpha");
		when(resourceStateResult.getResourceState()).thenReturn(mock(ResourceState.class));
		when(resourceStateResult.getResourceStateId()).thenReturn("Alpha");
		statesByPath = spy(new HashMap<String, Set<String>>());
		methodsByStateName = spy(new HashMap<String, Set<String>>(){
			private static final long serialVersionUID = 1L;
			{
				this.put("Alpha", new HashSet<String>(Arrays.asList(new String[]{"GET"})));
			}
		});
		statesByRequest = spy(new HashMap<String, String>());
		pathsByState = spy(new HashMap<String, String>());
		mapper = new ResourceStateMapper(statesByPath, methodsByStateName, 
				statesByRequest, pathsByState);
		//when
		this.mapper.map(resourceStateResult);
		this.mapper.map(resourceStateResult);
		//then
		verify(statesByPath, times(2)).put(eq("/alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"Alpha"}))));
		verify(methodsByStateName, times(2)).put(eq("Alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"GET", "POST"}))));
		verify(statesByRequest, times(2)).put(eq("GET /alpha"), eq("Alpha"));
		verify(statesByRequest, times(2)).put(eq("POST /alpha"), eq("Alpha"));
		verify(pathsByState, times(2)).put(eq("Alpha"), eq("/alpha"));
	}
	
	@Test
	public void testMapWithDuplicateResult(){
		//given
		ResourceStateResult resourceStateResult = mock(ResourceStateResult.class);
		when(resourceStateResult.getMethods()).thenReturn(new String[]{"GET", "POST"});
		when(resourceStateResult.getPath()).thenReturn("/alpha");
		when(resourceStateResult.getResourceState()).thenReturn(mock(ResourceState.class));
		when(resourceStateResult.getResourceStateId()).thenReturn("Alpha");
		statesByPath = spy(new HashMap<String, Set<String>>());
		methodsByStateName = spy(new HashMap<String, Set<String>>(){
			private static final long serialVersionUID = 1L;
			{
				this.put("Alpha", new HashSet<String>(Arrays.asList(new String[]{"GET"})));
			}
		});
		statesByRequest = spy(new HashMap<String, String>());
		pathsByState = spy(new HashMap<String, String>());
		mapper = new ResourceStateMapper(statesByPath, methodsByStateName, 
				statesByRequest, pathsByState);
		//when
		this.mapper.map(resourceStateResult);
		//then
		verify(statesByPath).put(eq("/alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"Alpha"}))));
		verify(methodsByStateName).put(eq("Alpha"), eq(new HashSet<String>(Arrays.asList(new String[]{"GET", "POST"}))));
		verify(statesByRequest).put(eq("GET /alpha"), eq("Alpha"));
		verify(statesByRequest).put(eq("POST /alpha"), eq("Alpha"));
		verify(pathsByState).put(eq("Alpha"), eq("/alpha"));
	}
}
