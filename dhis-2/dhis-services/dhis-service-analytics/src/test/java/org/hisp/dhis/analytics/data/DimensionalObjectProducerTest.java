/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.data;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hisp.dhis.DhisConvenienceTest.createCategory;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createIndicator;
import static org.hisp.dhis.DhisConvenienceTest.createIndicatorType;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnitGroup;
import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_APRIL;
import static org.hisp.dhis.analytics.DataQueryParams.DYNAMIC_DIM_CLASSES;
import static org.hisp.dhis.common.DimensionType.CATEGORY;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DisplayProperty.SHORTNAME;
import static org.hisp.dhis.common.IdScheme.NAME;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.feedback.ErrorCode.E7124;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_FINANCIAL_YEAR_START;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DimensionalObjectProducer}.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class DimensionalObjectProducerTest
{
    private DimensionalObjectProducer target;

    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DimensionService dimensionService;

    @Mock
    private AnalyticsSecurityManager securityManager;

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private AclService aclService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private I18nManager i18nManager;

    @Mock
    private I18n i18n;

    @Mock
    private I18nFormat i18nFormat;

    @BeforeEach
    public void setUp()
    {
        target = new DimensionalObjectProducer( idObjectManager, organisationUnitService,
            systemSettingManager, i18nManager, dimensionService, aclService, currentUserService );
    }

    @Test
    void testGetDimensionForIndicator()
    {
        // Given
        Indicator indicator = createIndicator( 'A', createIndicatorType( 'A' ) );
        String indicatorGroupUid = "oehv9EO3vP7";
        IndicatorGroup indicatorGroup = new IndicatorGroup( "dummy" );
        indicatorGroup.setUid( indicatorGroupUid );
        indicatorGroup.setCode( "CODE_10" );
        indicatorGroup.setMembers( Set.of( indicator ) );
        when( idObjectManager.getObject( IndicatorGroup.class, UID, indicatorGroupUid ) ).thenReturn( indicatorGroup );

        List<String> itemsUid = List.of( "IN_GROUP-" + indicatorGroupUid, "IN_GROUP-Behv9EO3hR1" );

        // When
        BaseDimensionalObject dimensionalObject = target.getDimension( itemsUid, UID );

        // Then
        assertEquals( "dx", dimensionalObject.getDimension() );
        assertEquals( "dx", dimensionalObject.getUid() );
        assertEquals( DATA_X, dimensionalObject.getDimensionType() );
        assertEquals( "Data", dimensionalObject.getDimensionDisplayName() );

        Indicator refIndicator = (Indicator) dimensionalObject.getItems().get( 0 );
        assertBaseDimensionalObjects( indicator, refIndicator );

        DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
        Keyword refKeyword = refDimensionKeywords.getKeywords().get( 0 );
        assertEquals( refKeyword.getKey(), indicatorGroup.getUid() );
        assertEquals( refKeyword.getMetadataItem().getName(), indicatorGroup.getName() );
        assertEquals( refKeyword.getMetadataItem().getCode(), indicatorGroup.getCode() );
        assertEquals( refKeyword.getMetadataItem().getUid(), indicatorGroup.getUid() );
    }

    @Test
    void testGetDimensionForDataElement()
    {
        // Given
        DataElement dataElement = createDataElement( 'A' );
        String deGroupUid = "oehv9EO3vP7";
        DataElementGroup dataElementGroup = new DataElementGroup( "dummy" );
        dataElementGroup.setUid( deGroupUid );
        dataElementGroup.setCode( "CODE_10" );
        dataElementGroup.setMembers( Set.of( dataElement, new DataElement() ) );

        when( idObjectManager.getObject( DataElementGroup.class, NAME, deGroupUid ) )
            .thenReturn( dataElementGroup );

        List<String> itemsUid = List.of( "DE_GROUP-" + deGroupUid, "DE_GROUP-Behv9EO3hR1" );

        // When
        BaseDimensionalObject dimensionalObject = target.getDimension( itemsUid, NAME );

        // Then
        assertEquals( "dx", dimensionalObject.getDimension() );
        assertEquals( "dx", dimensionalObject.getUid() );
        assertEquals( DATA_X, dimensionalObject.getDimensionType() );
        assertEquals( "Data", dimensionalObject.getDimensionDisplayName() );

        DataElement refDataElement = (DataElement) dimensionalObject.getItems().stream()
            .filter( de -> de.getUid() != null )
            .collect( toUnmodifiableList() ).get( 0 );

        assertBaseDimensionalObjects( dataElement, refDataElement );

        DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 0 ), dataElementGroup );
    }

    @Test
    void testGetDimensionWhenDataDimensionsAreNotFound()
    {
        // Given
        List<String> nonExistingItemsUid = List.of( "DE_GROUP-Achv9EO3hR1", "IN_GROUP-Behv9EO3hR1" );

        // Then
        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> target.getDimension( nonExistingItemsUid, UID ) );
        assertEquals( E7124, ex.getErrorCode() );
    }

    @Test
    void testGetOrgUnitDimensionWithNoLevelsNoGroup()
    {
        // Given
        OrganisationUnit level2Ou1 = createOrganisationUnit( "Bo" );
        OrganisationUnit level2Ou2 = createOrganisationUnit( "Bombali" );
        OrganisationUnit ou1 = createOrganisationUnit( 'A' );
        OrganisationUnit ou2 = createOrganisationUnit( 'B' );
        List<OrganisationUnit> organisationUnits = new ArrayList<>( asList( level2Ou1, level2Ou2, ou1, ou2 ) );

        when( organisationUnitService.getOrganisationUnitsAtLevels( anyList(), anyList() ) )
            .thenReturn( new ArrayList<>( asList( ou1, ou2 ) ) );

        List<String> itemsUid = List.of( "USER_ORGUNIT", "USER_ORGUNIT_CHILDREN", "USER_ORGUNIT_GRANDCHILDREN",
            "LEVEL-" + ou1.getUid(), "OU_GROUP-" + ou2.getUid() );

        // When
        BaseDimensionalObject dimensionalObject = target.getOrgUnitDimension( itemsUid, SHORTNAME, organisationUnits,
            UID );

        // Then
        assertEquals( "ou", dimensionalObject.getDimension() );
        assertEquals( "ou", dimensionalObject.getUid() );
        assertEquals( ORGANISATION_UNIT, dimensionalObject.getDimensionType() );
        assertEquals( "Organisation unit", dimensionalObject.getDimensionDisplayName() );
        assertTrue( dimensionalObject.getDimensionItemKeywords().isEmpty() );

        sort( dimensionalObject.getItems() );
        OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get( 0 );
        OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get( 1 );
        assertOrgUnits( ou1, refOu1 );
        assertOrgUnits( ou2, refOu2 );
    }

    @Test
    void testGetOrgUnitDimensionWithWithLevelAndGroup()
    {
        // Given
        OrganisationUnitGroup organisationUnitGroup = createOrganisationUnitGroup( 'A' );
        OrganisationUnit level2Ou1 = createOrganisationUnit( "Bo" );
        OrganisationUnit level2Ou2 = createOrganisationUnit( "Bombali" );
        OrganisationUnit ou1 = createOrganisationUnit( 'A' );
        OrganisationUnit ou2 = createOrganisationUnit( 'B' );
        List<OrganisationUnit> organisationUnits = new ArrayList<>( asList( level2Ou1, level2Ou2, ou1, ou2 ) );

        when( organisationUnitService.getOrganisationUnitsAtLevels( anyList(), anyList() ) )
            .thenReturn( new ArrayList<>( asList( level2Ou1, level2Ou2 ) ) );
        when( organisationUnitService.getOrganisationUnitLevelByLevelOrUid( level2Ou1.getName() ) ).thenReturn( 1 );
        when( idObjectManager.getObject( OrganisationUnitGroup.class, UID, level2Ou2.getUid() ) )
            .thenReturn( organisationUnitGroup );

        List<String> itemsUid = List.of( "USER_ORGUNIT", "USER_ORGUNIT_CHILDREN", "USER_ORGUNIT_GRANDCHILDREN",
            "LEVEL-" + level2Ou1.getName(), "OU_GROUP-" + level2Ou2.getUid() );

        // When
        BaseDimensionalObject dimensionalObject = target.getOrgUnitDimension( itemsUid, DisplayProperty.NAME,
            organisationUnits,
            UID );

        // Then
        assertEquals( "ou", dimensionalObject.getDimension() );
        assertEquals( "ou", dimensionalObject.getUid() );
        assertEquals( ORGANISATION_UNIT, dimensionalObject.getDimensionType() );
        assertEquals( "Organisation unit", dimensionalObject.getDimensionDisplayName() );

        sort( dimensionalObject.getItems() );
        OrganisationUnit refOu1 = (OrganisationUnit) dimensionalObject.getItems().get( 0 );
        OrganisationUnit refOu2 = (OrganisationUnit) dimensionalObject.getItems().get( 1 );
        assertOrgUnits( level2Ou1, refOu1 );
        assertOrgUnits( level2Ou2, refOu2 );

        DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 0 ), organisationUnitGroup );
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 1 ), level2Ou1 );
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 2 ), level2Ou2 );
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 3 ), ou1 );
        assertKeywordForDimensionalObject( refDimensionKeywords.getKeywords().get( 4 ), ou2 );
    }

    @Test
    void testGetDimensionWhenOrgUnitsAreNotFound()
    {
        // Given
        List<String> nonExistingItemsUid = List.of( "LEVEL-Achv9EO3hR1", "OU_GROUP-Blhv9EO3hR1" );

        // Then
        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> target.getDimension( nonExistingItemsUid, UID ) );
        assertEquals( E7124, ex.getErrorCode() );
    }

    @Test
    void testGetOrgUnitGroupDimension()
    {
        // Given
        OrganisationUnitGroup organisationUnitGroup1 = createOrganisationUnitGroup( 'A' );
        OrganisationUnitGroup organisationUnitGroup2 = createOrganisationUnitGroup( 'B' );

        when( idObjectManager.getObject( OrganisationUnitGroup.class, UID, "Achv9EO3hR1" ) )
            .thenReturn( organisationUnitGroup1 );
        when( idObjectManager.getObject( OrganisationUnitGroup.class, UID, "Blhv9EO3hR1" ) )
            .thenReturn( organisationUnitGroup2 );

        List<String> itemsUid = List.of( "Achv9EO3hR1", "Blhv9EO3hR1" );

        // When
        BaseDimensionalObject dimensionalObject = target.getOrgUnitGroupDimension( itemsUid, UID );

        // Then
        assertEquals( "oug", dimensionalObject.getDimension() );
        assertEquals( "oug", dimensionalObject.getUid() );
        assertEquals( ORGANISATION_UNIT_GROUP, dimensionalObject.getDimensionType() );
        assertEquals( "Organisation unit group", dimensionalObject.getDimensionDisplayName() );

        assertNull( dimensionalObject.getDimensionItemKeywords() );
        assertBaseDimensionalObjects( organisationUnitGroup1,
            (OrganisationUnitGroup) dimensionalObject.getItems().get( 0 ) );
        assertBaseDimensionalObjects( organisationUnitGroup2,
            (OrganisationUnitGroup) dimensionalObject.getItems().get( 1 ) );
    }

    @Test
    void testGetPeriodDimensions()
    {
        // Given
        List<String> itemsUid = List.of( "LAST_YEAR:LAST_UPDATED", "LAST_5_YEARS:SCHEDULED_DATE" );

        when( systemSettingManager.getSystemSetting( ANALYTICS_FINANCIAL_YEAR_START,
            AnalyticsFinancialYearStartKey.class ) ).thenReturn( FINANCIAL_YEAR_APRIL );
        when( i18nManager.getI18nFormat() ).thenReturn( i18nFormat );
        when( i18nManager.getI18n() ).thenReturn( i18n );

        // When
        BaseDimensionalObject dimensionalObject = target.getPeriodDimension( itemsUid, new Date() );

        // Then
        assertEquals( "pe", dimensionalObject.getDimension() );
        assertEquals( "pe", dimensionalObject.getUid() );
        assertEquals( PERIOD, dimensionalObject.getDimensionType() );
        assertEquals( "Period", dimensionalObject.getDimensionDisplayName() );

        DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
        assertEquals( "LAST_YEAR", refDimensionKeywords.getKeywords().get( 0 ).getKey() );
        assertEquals( "LAST_5_YEARS", refDimensionKeywords.getKeywords().get( 1 ).getKey() );

        List<DimensionalItemObject> refPeriods = dimensionalObject.getItems();
        assertDailyPeriod( "2017", "SCHEDULED_DATE", "2017-01-01", "2017-12-31", (Period) refPeriods.get( 0 ) );
        assertDailyPeriod( "2018", "SCHEDULED_DATE", "2018-01-01", "2018-12-31", (Period) refPeriods.get( 1 ) );
        assertDailyPeriod( "2019", "SCHEDULED_DATE", "2019-01-01", "2019-12-31", (Period) refPeriods.get( 2 ) );
        assertDailyPeriod( "2020", "SCHEDULED_DATE", "2020-01-01", "2020-12-31", (Period) refPeriods.get( 3 ) );
        assertDailyPeriod( "2021", "LAST_UPDATED", "2021-01-01", "2021-12-31", (Period) refPeriods.get( 4 ) );
        assertDailyPeriod( "2021", "SCHEDULED_DATE", "2021-01-01", "2021-12-31", (Period) refPeriods.get( 5 ) );
    }

    private void assertDailyPeriod( String year, String dateField, String startDate, String endDate, Period period )
    {
        assertTrue( period.getPeriodType() instanceof YearlyPeriodType );
        assertEquals( year, period.getIsoDate() );
        assertEquals( dateField, period.getDateField() );
        assertEquals( startDate, period.getStartDateString() );
        assertEquals( endDate, period.getEndDateString() );
    }

    @Test
    void testGetPeriodDimensionForNonIsoPeriod()
    {
        // Given
        List<String> itemsUid = List.of( "2021-05-01_2021-06-01:LAST_UPDATED" );

        when( systemSettingManager.getSystemSetting( ANALYTICS_FINANCIAL_YEAR_START,
            AnalyticsFinancialYearStartKey.class ) ).thenReturn( FINANCIAL_YEAR_APRIL );
        when( i18nManager.getI18nFormat() ).thenReturn( i18nFormat );

        // When
        BaseDimensionalObject dimensionalObject = target.getPeriodDimension( itemsUid, new Date() );

        // Then
        assertEquals( "pe", dimensionalObject.getDimension() );
        assertEquals( "pe", dimensionalObject.getUid() );
        assertEquals( PERIOD, dimensionalObject.getDimensionType() );
        assertEquals( "Period", dimensionalObject.getDimensionDisplayName() );

        DimensionItemKeywords refDimensionKeywords = dimensionalObject.getDimensionItemKeywords();
        assertEquals( "2021-05-01_2021-06-01", refDimensionKeywords.getKeywords().get( 0 ).getKey() );

        List<DimensionalItemObject> refPeriods = dimensionalObject.getItems();
        assertTrue( ((Period) refPeriods.get( 0 )).getPeriodType() instanceof DailyPeriodType );
        assertEquals( "20210501", ((Period) refPeriods.get( 0 )).getIsoDate() );
        assertEquals( "LAST_UPDATED", ((Period) refPeriods.get( 0 )).getDateField() );
        assertEquals( "2021-05-01", ((Period) refPeriods.get( 0 )).getStartDateString() );
        assertEquals( "2021-06-01", ((Period) refPeriods.get( 0 )).getEndDateString() );
    }

    @Test
    void testDynamicFrom()
    {
        // Given
        String categoryUid = "L6BswcbPGqs";
        String categoryName = "Category-A";
        Category category = createCategory( categoryName, categoryUid );
        List<String> itemsUid = List.of( categoryUid );

        when( idObjectManager.get( DYNAMIC_DIM_CLASSES, UID, categoryUid ) ).thenReturn( category );

        // When
        Optional<BaseDimensionalObject> dimensionalObject = target.getDynamicDimension( categoryUid, itemsUid,
            DisplayProperty.NAME, UID );

        // Then
        assertEquals( categoryUid, dimensionalObject.get().getDimension() );
        assertEquals( categoryUid, dimensionalObject.get().getUid() );
        assertEquals( CATEGORY, dimensionalObject.get().getDimensionType() );
        assertEquals( categoryName, dimensionalObject.get().getDimensionDisplayName() );
        assertTrue( dimensionalObject.get().getItems().isEmpty() );
        assertNull( dimensionalObject.get().getDimensionItemKeywords() );
    }

    private void assertKeywordForDimensionalObject( Keyword keyword, BaseDimensionalItemObject dimensionalItemObject )
    {
        assertEquals( keyword.getKey(), dimensionalItemObject.getUid() );
        assertEquals( keyword.getMetadataItem().getName(), dimensionalItemObject.getName() );
        assertEquals( keyword.getMetadataItem().getCode(), dimensionalItemObject.getCode() );
        assertEquals( keyword.getMetadataItem().getUid(), dimensionalItemObject.getUid() );
    }

    private void assertOrgUnits( OrganisationUnit expected, OrganisationUnit actual )
    {
        assertEquals( expected.getComment(), actual.getComment() );
        assertEquals( expected.getShortName(), actual.getShortName() );
        assertEquals( expected.getUid(), actual.getUid() );
        assertEquals( expected.getCode(), actual.getCode() );
        assertEquals( expected.getName(), actual.getName() );
    }

    private void assertBaseDimensionalObjects( BaseDimensionalItemObject expected, BaseDimensionalItemObject actual )
    {
        assertEquals( expected.getShortName(), actual.getShortName() );
        assertEquals( expected.getUid(), actual.getUid() );
        assertEquals( expected.getCode(), actual.getCode() );
        assertEquals( expected.getName(), actual.getName() );
        assertEquals( expected.getDescription(), actual.getDescription() );
        assertEquals( expected.hashCode(), actual.hashCode() );
    }
}
